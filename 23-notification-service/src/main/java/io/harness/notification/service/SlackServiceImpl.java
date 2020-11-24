package io.harness.notification.service;

import static io.harness.NotificationRequest.Slack;
import static io.harness.NotificationServiceConstants.TEST_SLACK_TEMPLATE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.exception.WingsException.USER;

import static org.apache.commons.lang3.StringUtils.stripToNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import io.harness.NotificationRequest;
import io.harness.Team;
import io.harness.notification.NotificationChannelType;
import io.harness.notification.exception.NotificationException;
import io.harness.notification.remote.dto.NotificationSettingDTO;
import io.harness.notification.remote.dto.SlackSettingDTO;
import io.harness.notification.service.api.ChannelService;
import io.harness.notification.service.api.NotificationSettingsService;
import io.harness.notification.service.api.NotificationTemplateService;

import com.google.inject.Inject;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.text.StrSubstitutor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class SlackServiceImpl implements ChannelService {
  public static final MediaType APPLICATION_JSON = MediaType.parse("application/json; charset=utf-8");

  private final NotificationSettingsService notificationSettingsService;
  private final OkHttpClient client = new OkHttpClient();
  private final NotificationTemplateService notificationTemplateService;

  @Override
  public boolean send(NotificationRequest notificationRequest) {
    if (Objects.isNull(notificationRequest) || !notificationRequest.hasSlack()) {
      return false;
    }

    String notificationId = notificationRequest.getId();
    NotificationRequest.Slack slackDetails = notificationRequest.getSlack();
    String templateId = slackDetails.getTemplateId();
    Map<String, String> templateData = slackDetails.getTemplateDataMap();

    if (Objects.isNull(trimToNull(templateId))) {
      log.info("template Id is null for notification request {}", notificationId);
      return false;
    }

    List<String> slackWebhookUrls = getRecipients(notificationRequest);
    if (isEmpty(slackWebhookUrls)) {
      log.info("No slackWebhookUrls found in notification request {}", notificationId);
      return false;
    }

    return send(slackWebhookUrls, templateId, templateData, notificationRequest.getId(), notificationRequest.getTeam());
  }

  @Override
  public boolean sendTestNotification(NotificationSettingDTO notificationSettingDTO) {
    SlackSettingDTO slackSettingDTO = (SlackSettingDTO) notificationSettingDTO;
    String webhookUrl = slackSettingDTO.getRecipient();
    if (Objects.isNull(stripToNull(webhookUrl))) {
      throw new NotificationException("Malformed webhook Url " + webhookUrl, DEFAULT_ERROR_CODE, USER);
    }
    boolean sent = send(Collections.singletonList(webhookUrl), TEST_SLACK_TEMPLATE, Collections.emptyMap(),
        slackSettingDTO.getNotificationId(), null);
    if (!sent) {
      throw new NotificationException("Invalid webhook url " + webhookUrl, DEFAULT_ERROR_CODE, USER);
    }
    return true;
  }

  private boolean send(List<String> slackWebhookUrls, String templateId, Map<String, String> templateData,
      String notificationId, Team team) {
    Optional<String> templateOpt = notificationTemplateService.getTemplateAsString(templateId, team);
    if (!templateOpt.isPresent()) {
      log.info("Can't find template with templateId {} for notification request {}", templateId, notificationId);
      return false;
    }
    String template = templateOpt.get();
    StrSubstitutor strSubstitutor = new StrSubstitutor(templateData);
    String message = strSubstitutor.replace(template);

    boolean sent = false;
    for (String webhookUrl : slackWebhookUrls) {
      boolean ret = sendJSONMessage(message, webhookUrl);
      sent = sent || ret;
    }
    log.info(sent ? "Notificaition request {} sent" : "Failed to send notification for request {}", notificationId);
    return sent;
  }

  private List<String> getRecipients(NotificationRequest notificationRequest) {
    Slack slackChannelDetails = notificationRequest.getSlack();
    List<String> recipients = new ArrayList<>(slackChannelDetails.getSlackWebHookUrlsList());
    List<String> slackWebHookUrls = notificationSettingsService.getNotificationSettingsForGroups(
        slackChannelDetails.getUserGroupIdsList(), NotificationChannelType.SLACK, notificationRequest.getAccountId());
    recipients.addAll(slackWebHookUrls);
    return recipients;
  }

  private boolean sendJSONMessage(String message, String slackWebhook) {
    try {
      RequestBody body = RequestBody.create(APPLICATION_JSON, message);
      Request request = new Request.Builder()
                            .url(slackWebhook)
                            .post(body)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Accept", "*/*")
                            .addHeader("Cache-Control", "no-cache")
                            .addHeader("Host", "hooks.slack.com")
                            .addHeader("accept-encoding", "gzip, deflate")
                            .addHeader("content-length", "798")
                            .addHeader("Connection", "keep-alive")
                            .addHeader("cache-control", "no-cache")
                            .build();

      try (Response response = client.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          String bodyString = (null != response.body()) ? response.body().string() : "null";
          log.error("Response not Successful. Response body: {}", bodyString);
          return false;
        }
        return true;
      }
    } catch (Exception e) {
      log.error("Error sending post data", e);
    }
    return false;
  }
}
