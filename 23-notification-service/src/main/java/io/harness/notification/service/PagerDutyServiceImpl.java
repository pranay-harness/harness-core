package io.harness.notification.service;

import static io.harness.NotificationClientConstants.HARNESS_NAME;
import static io.harness.NotificationRequest.PagerDuty;
import static io.harness.NotificationServiceConstants.TEST_PD_TEMPLATE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.exception.WingsException.USER;

import static org.apache.commons.lang3.StringUtils.stripToNull;

import io.harness.NotificationRequest;
import io.harness.Team;
import io.harness.notification.NotificationChannelType;
import io.harness.notification.exception.NotificationException;
import io.harness.notification.remote.dto.NotificationSettingDTO;
import io.harness.notification.remote.dto.PagerDutySettingDTO;
import io.harness.notification.service.api.ChannelService;
import io.harness.notification.service.api.NotificationSettingsService;
import io.harness.notification.service.api.NotificationTemplateService;
import io.harness.serializer.YamlUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.dikhan.pagerduty.client.events.PagerDutyEventsClient;
import com.github.dikhan.pagerduty.client.events.domain.*;
import com.github.dikhan.pagerduty.client.events.domain.TriggerIncident.TriggerIncidentBuilder;
import com.github.dikhan.pagerduty.client.events.exceptions.NotifyEventException;
import com.google.inject.Inject;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;
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

  @Override
  public boolean send(NotificationRequest notificationRequest) {
    if (Objects.isNull(notificationRequest) || !notificationRequest.hasPagerDuty()) {
      return false;
    }

    String notificationId = notificationRequest.getId();
    PagerDuty pagerDutyDetails = notificationRequest.getPagerDuty();
    String templateId = pagerDutyDetails.getTemplateId();
    Map<String, String> templateData = pagerDutyDetails.getTemplateDataMap();

    if (Objects.isNull(stripToNull(templateId))) {
      log.info("template Id is null for notification request {}", notificationId);
      return false;
    }

    List<String> pagerDutyKeys = getRecipients(notificationRequest);
    if (isEmpty(pagerDutyKeys)) {
      log.info("No pagerduty integration key found in notification request {}", notificationId);
      return false;
    }

    return send(pagerDutyKeys, templateId, templateData, notificationRequest.getId(), notificationRequest.getTeam());
  }

  @Override
  public boolean sendTestNotification(NotificationSettingDTO notificationSettingDTO) {
    PagerDutySettingDTO pagerDutySettingDTO = (PagerDutySettingDTO) notificationSettingDTO;
    String pagerdutyKey = pagerDutySettingDTO.getRecipient();
    if (Objects.isNull(stripToNull(pagerdutyKey))) {
      throw new NotificationException("Malformed pagerduty key " + pagerdutyKey, DEFAULT_ERROR_CODE, USER);
    }
    boolean sent = send(Collections.singletonList(pagerdutyKey), TEST_PD_TEMPLATE, Collections.emptyMap(),
        pagerDutySettingDTO.getNotificationId(), null);
    if (!sent) {
      throw new NotificationException("Invalid pagerduty key " + pagerdutyKey, DEFAULT_ERROR_CODE, USER);
    }
    return true;
  }

  private boolean send(List<String> pagerDutyKeys, String templateId, Map<String, String> templateData,
      String notificationId, Team team) {
    Optional<PagerDutyTemplate> templateOpt = getTemplate(templateId, team);
    if (!templateOpt.isPresent()) {
      log.info("Can't find template with templateId {} for notification request {}", templateId, notificationId);
      return false;
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

    boolean sent = false;
    PagerDutyEventsClient pagerDutyEventsClient = PagerDutyEventsClient.create();
    for (String pagerDutyKey : pagerDutyKeys) {
      TriggerIncident incident = TriggerIncidentBuilder.newBuilder(pagerDutyKey, payload).setLinks(links).build();
      try {
        EventResult result = pagerDutyEventsClient.trigger(incident);
        sent = sent || (result != null && result.getErrors() == null);
      } catch (NotifyEventException e) {
        log.error("Unable to send PagerDuty incident for notification reference id {}", notificationId);
      }
    }
    log.info(sent ? "Notificaition request {} sent" : "Failed to send notification for request {}", notificationId);
    return sent;
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
