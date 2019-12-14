package software.wings.service.impl;

import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.FeatureName;
import software.wings.beans.SlackMessage;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.notification.SlackNotificationConfiguration;
import software.wings.beans.notification.SlackNotificationSetting;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SlackMessageSender;
import software.wings.service.intfc.SlackNotificationService;

import java.util.List;
import java.util.Objects;

/**
 * Created by anubhaw on 12/14/16.
 */

@Slf4j
@Singleton
public class SlackNotificationServiceImpl implements SlackNotificationService {
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private SlackMessageSender slackMessageSender;
  @Inject private FeatureFlagService featureFlagService;

  public static final String SLACK_WEBHOOK_URL_PREFIX = "https://hooks.slack.com/services/";
  public static final MediaType APPLICATION_JSON = MediaType.parse("application/json; charset=utf-8");

  @Override
  public void sendMessage(SlackNotificationConfiguration slackConfig, String slackChannel, String senderName,
      String message, String accountId) {
    if (Objects.requireNonNull(slackConfig, "slack Config can't be null")
            .equals(SlackNotificationSetting.emptyConfig())) {
      return;
    }

    String webhookUrl = slackConfig.getOutgoingWebhookUrl();
    if (StringUtils.isEmpty(webhookUrl)) {
      logger.error("Webhook URL is empty. No message will be sent. Config: {}, Message: {}", slackConfig, message);
      return;
    }

    if (featureFlagService.isEnabled(FeatureName.SEND_SLACK_NOTIFICATION_FROM_DELEGATE, accountId)) {
      try {
        logger.info("Sending message via delegate");
        SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                              .accountId(accountId)
                                              .appId(GLOBAL_APP_ID)
                                              .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                              .build();
        logger.info("Sending message for account {} via delegate", accountId);
        delegateProxyFactory.get(SlackMessageSender.class, syncTaskContext)
            .send(new SlackMessage(slackConfig.getOutgoingWebhookUrl(), slackChannel, senderName, message), true);
      } catch (Exception ex) {
        logger.error("Failed to send slack message", ex);
      }
    } else {
      logger.info("Sending message for account {} via manager", accountId);
      slackMessageSender.send(
          new SlackMessage(slackConfig.getOutgoingWebhookUrl(), slackChannel, senderName, message), false);
    }
  }

  @Override
  public void sendJSONMessage(String message, List<String> slackWebhooks) {
    for (String slackWebHook : slackWebhooks) {
      try {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(APPLICATION_JSON, message);
        Request request = new Request.Builder()
                              .url(slackWebHook)
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
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
          String bodyString = (null != response.body()) ? response.body().string() : "null";

          logger.error("Response not Successful. Response body: {}", bodyString);
        }
      } catch (Exception e) {
        logger.error("Error sending post data", e);
      }
    }
  }
}