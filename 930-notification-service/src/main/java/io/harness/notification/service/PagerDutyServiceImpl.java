package io.harness.notification.service;

import static io.harness.NotificationRequest.PagerDuty;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.exception.WingsException.USER;
import static io.harness.notification.constant.NotificationClientConstants.HARNESS_NAME;
import static io.harness.notification.constant.NotificationServiceConstants.TEST_PD_TEMPLATE;

import static org.apache.commons.lang3.StringUtils.stripToNull;

import io.harness.NotificationRequest;
import io.harness.Team;
import io.harness.beans.DelegateTaskRequest;
import io.harness.delegate.beans.NotificationTaskResponse;
import io.harness.delegate.beans.PagerDutyTaskParams;
import io.harness.notification.NotificationChannelType;
import io.harness.notification.beans.NotificationProcessingResponse;
import io.harness.notification.exception.NotificationException;
import io.harness.notification.remote.dto.NotificationSettingDTO;
import io.harness.notification.remote.dto.PagerDutySettingDTO;
import io.harness.notification.service.api.ChannelService;
import io.harness.notification.service.api.NotificationSettingsService;
import io.harness.notification.service.api.NotificationTemplateService;
import io.harness.notification.service.senders.PagerDutySenderImpl;
import io.harness.serializer.YamlUtils;
import io.harness.service.DelegateGrpcClientWrapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.dikhan.pagerduty.client.events.domain.LinkContext;
import com.github.dikhan.pagerduty.client.events.domain.Payload;
import com.github.dikhan.pagerduty.client.events.domain.Severity;
import com.google.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StrSubstitutor;
import org.apache.commons.text.WordUtils;
import org.json.JSONObject;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class PagerDutyServiceImpl implements ChannelService {
  private final NotificationSettingsService notificationSettingsService;
  private final NotificationTemplateService notificationTemplateService;
  private final YamlUtils yamlUtils;
  private final PagerDutySenderImpl pagerDutySender;
  private final DelegateGrpcClientWrapper delegateGrpcClientWrapper;

  @Override
  public NotificationProcessingResponse send(NotificationRequest notificationRequest) {
    if (Objects.isNull(notificationRequest) || !notificationRequest.hasPagerDuty()) {
      return NotificationProcessingResponse.trivialResponseWithNoRetries;
    }

    String notificationId = notificationRequest.getId();
    PagerDuty pagerDutyDetails = notificationRequest.getPagerDuty();
    String templateId = pagerDutyDetails.getTemplateId();
    Map<String, String> templateData = pagerDutyDetails.getTemplateDataMap();

    if (Objects.isNull(stripToNull(templateId))) {
      log.info("template Id is null for notification request {}", notificationId);
      return NotificationProcessingResponse.trivialResponseWithNoRetries;
    }

    List<String> pagerDutyKeys = getRecipients(notificationRequest);
    if (isEmpty(pagerDutyKeys)) {
      log.info("No pagerduty integration key found in notification request {}", notificationId);
      return NotificationProcessingResponse.trivialResponseWithNoRetries;
    }

    return send(pagerDutyKeys, templateId, templateData, notificationRequest.getId(), notificationRequest.getTeam(),
        notificationRequest.getAccountId());
  }

  @Override
  public boolean sendTestNotification(NotificationSettingDTO notificationSettingDTO) {
    PagerDutySettingDTO pagerDutySettingDTO = (PagerDutySettingDTO) notificationSettingDTO;
    String pagerdutyKey = pagerDutySettingDTO.getRecipient();
    if (Objects.isNull(stripToNull(pagerdutyKey)) || Objects.isNull(stripToNull(pagerDutySettingDTO.getAccountId()))) {
      throw new NotificationException("Malformed pagerduty key encountered while processing Test Connection request "
              + notificationSettingDTO.getNotificationId(),
          DEFAULT_ERROR_CODE, USER);
    }
    NotificationProcessingResponse processingResponse = send(Collections.singletonList(pagerdutyKey), TEST_PD_TEMPLATE,
        Collections.emptyMap(), pagerDutySettingDTO.getNotificationId(), null, notificationSettingDTO.getAccountId());
    if (NotificationProcessingResponse.isNotificationResquestFailed(processingResponse)) {
      throw new NotificationException("Invalid pagerduty key encountered while processing Test Connection request "
              + notificationSettingDTO.getNotificationId(),
          DEFAULT_ERROR_CODE, USER);
    }
    return true;
  }

  private NotificationProcessingResponse send(List<String> pagerDutyKeys, String templateId,
      Map<String, String> templateData, String notificationId, Team team, String accountId) {
    Optional<PagerDutyTemplate> templateOpt = getTemplate(templateId, team);
    if (!templateOpt.isPresent()) {
      log.info("Can't find template with templateId {} for notification request {}", templateId, notificationId);
      return NotificationProcessingResponse.trivialResponseWithNoRetries;
    }
    PagerDutyTemplate template = templateOpt.get();

    StrSubstitutor strSubstitutor = new StrSubstitutor(templateData);
    String summary = template.getSummary();
    summary = strSubstitutor.replace(summary);

    List<LinkContext> links = new ArrayList<>();
    Map<String, String> customDetails =
        templateData.keySet()
            .stream()
            .filter(key -> !key.equalsIgnoreCase("START_TS_SECS") && !key.equalsIgnoreCase("END_TS_SECS"))
            .collect(Collectors.toMap(
                key -> WordUtils.capitalizeFully(key.replace("_", " ")), templateData::get, (a, b) -> b));

    JSONObject jsonObject = new JSONObject(customDetails);
    Payload payload = Payload.Builder.newBuilder()
                          .setTimestamp(OffsetDateTime.now())
                          .setSeverity(Severity.ERROR)
                          .setCustomDetails(jsonObject)
                          .setSummary(summary)
                          .setSource(HARNESS_NAME)
                          .build();

    if (Objects.nonNull(template.getLink())) {
      String linkHref = template.getLink().getHref();
      String linkText = template.getLink().getText();
      LinkContext linkContext = new LinkContext(linkHref, linkText);
      links.add(linkContext);
    }

    NotificationProcessingResponse processingResponse = null;
    if (notificationSettingsService.getSendNotificationViaDelegate(accountId)) {
      DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                    .accountId(accountId)
                                                    .taskType("NOTIFY_PAGERDUTY")
                                                    .taskParameters(PagerDutyTaskParams.builder()
                                                                        .notificationId(notificationId)
                                                                        .pagerDutyKeys(pagerDutyKeys)
                                                                        .payload(payload)
                                                                        .links(links)
                                                                        .build())
                                                    .executionTimeout(Duration.ofMinutes(1L))
                                                    .build();
      NotificationTaskResponse notificationTaskResponse =
          (NotificationTaskResponse) delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
      processingResponse = notificationTaskResponse.getProcessingResponse();
    } else {
      processingResponse = pagerDutySender.send(pagerDutyKeys, payload, links, notificationId);
    }
    log.info(NotificationProcessingResponse.isNotificationResquestFailed(processingResponse)
            ? "Notification request {} sent"
            : "Failed to send notification for request {}",
        notificationId);
    return processingResponse;
  }

  private Optional<PagerDutyTemplate> getTemplate(String templateId, Team team) {
    Optional<String> templateStrOptional = notificationTemplateService.getTemplateAsString(templateId, team);
    if (!templateStrOptional.isPresent()) {
      return Optional.empty();
    }
    PagerDutyTemplate pagerDutyTemplate = null;
    try {
      pagerDutyTemplate = yamlUtils.read(templateStrOptional.get(), new TypeReference<PagerDutyTemplate>() {});
    } catch (IOException e) {
      log.error("Failed to parse the saved file into yaml. check format for {}", templateId);
    }
    return Optional.ofNullable(pagerDutyTemplate);
  }

  private List<String> getRecipients(NotificationRequest notificationRequest) {
    PagerDuty pagerDutyDetails = notificationRequest.getPagerDuty();
    List<String> recipients = new ArrayList<>(pagerDutyDetails.getPagerDutyIntegrationKeysList());
    List<String> pagerDutyKeys = notificationSettingsService.getNotificationSettingsForGroups(
        pagerDutyDetails.getUserGroupIdsList(), NotificationChannelType.PAGERDUTY, notificationRequest.getAccountId());
    recipients.addAll(pagerDutyKeys);
    return recipients;
  }

  @Getter
  @Setter
  public static class Link {
    private String href;
    private String text;
  }

  @Getter
  @Setter
  public static class PagerDutyTemplate {
    private String summary;
    private Link link;
  }
}
