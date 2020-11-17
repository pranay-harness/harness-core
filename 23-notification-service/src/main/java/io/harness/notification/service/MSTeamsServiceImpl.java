package io.harness.notification.service;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.NotificationRequest;
import io.harness.Team;
import io.harness.notification.NotificationChannelType;
import io.harness.notification.remote.dto.MSTeamSettingDTO;
import io.harness.notification.remote.dto.NotificationSettingDTO;
import io.harness.notification.service.api.MSTeamsService;
import io.harness.notification.service.api.NotificationSettingsService;
import io.harness.notification.service.api.NotificationTemplateService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StrSubstitutor;
import org.apache.commons.validator.routines.UrlValidator;

import java.util.*;
import java.util.regex.Pattern;

import static io.harness.NotificationConstants.*;
import static io.harness.NotificationRequest.MSTeam;
import static io.harness.NotificationServiceConstants.TEST_MICROSOFTTEAMS_TEMPLATE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.lang.String.format;
import static java.lang.String.join;
import static org.apache.commons.lang3.StringUtils.*;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class MSTeamsServiceImpl implements MSTeamsService {
  private static final String ARTIFACTS = "ARTIFACTS";
  private static final String ASTERISK = "\\*";
  private static final String ASTERISK_REPLACEMENT = "**";
  private static final String COMMA = ",";
  private static final String KEY_VERB = "VERB";
  private static final String NA = "N / A";
  private static final String NAME = "_NAME";
  private static final String NEW_LINE = "\\n";
  private static final String NEW_LINE_REPLACEMENT = "\n\n";
  private static final String PIPELINE = "PIPELINE";
  private static final String[] schemes = {"https", "http", "rtsp", "ftp"};
  private static final List<String> TEMPLATE_KEYS_TO_BE_PROCESSED =
      ImmutableList.of("APPLICATION", ARTIFACTS, "ENVIRONMENT", PIPELINE, "SERVICE", "TRIGGER");
  private static final String THEME_COLOR = "THEME_COLOR";
  private static final String UNDERSCORE_REPLACEMENT = "\\\\\\\\\\_";
  private static final String UNDERSCORE = "_";
  private static final String URL = "_URL";
  private static final Pattern placeHolderPattern = Pattern.compile("\\$\\{.+?}");

  private final MicrosoftTeamsSenderServiceImpl microsoftTeamsSenderServiceImpl;
  private final NotificationSettingsService notificationSettingsService;
  private final NotificationTemplateService notificationTemplateService;

  @Override
  public boolean send(NotificationRequest notificationRequest) {
    if (Objects.isNull(notificationRequest) || !notificationRequest.hasMsTeam()) {
      return false;
    }

    String notificationId = notificationRequest.getId();
    MSTeam msTeamDetails = notificationRequest.getMsTeam();
    String templateId = msTeamDetails.getTemplateId();
    Map<String, String> templateData = msTeamDetails.getTemplateDataMap();

    if (Objects.isNull(trimToNull(templateId))) {
      log.info("template Id is null for notification request {}", notificationId);
      return false;
    }

    List<String> microsoftTeamsWebhookUrls = getRecipients(notificationRequest);
    if (isEmpty(microsoftTeamsWebhookUrls)) {
      log.info("No microsoft teams webhook url found in notification request {}", notificationId);
      return false;
    }

    return send(microsoftTeamsWebhookUrls, templateId, templateData, notificationId, notificationRequest.getTeam());
  }

  @Override
  public boolean sendTestNotification(NotificationSettingDTO notificationSettingDTO) {
    MSTeamSettingDTO msTeamSettingDTO = (MSTeamSettingDTO) notificationSettingDTO;
    return send(Collections.singletonList(msTeamSettingDTO.getRecipient()), TEST_MICROSOFTTEAMS_TEMPLATE,
        Collections.emptyMap(), msTeamSettingDTO.getNotificationId(), null);
  }

  @Override
  public boolean send(List<String> microsoftTeamsWebhookUrls, String templateId, Map<String, String> templateData,
      String notificationId) {
    return send(microsoftTeamsWebhookUrls, templateId, templateData, notificationId, null);
  }

  @Override
  public boolean send(List<String> microsoftTeamsWebhookUrls, String templateId, Map<String, String> templateData,
      String notificationId, Team team) {
    Optional<String> templateOpt = notificationTemplateService.getTemplateAsString(templateId, team);
    if (!templateOpt.isPresent()) {
      log.info("Can't find template with templateId {} for notification request {}", templateId, notificationId);
      return false;
    }
    String template = templateOpt.get();

    templateData = processTemplateVariables(templateData);
    Optional<String> messageOpt = getDecoratedNotificationMessage(template, templateData);
    if (!messageOpt.isPresent()) {
      log.error(
          "Can't qualify the template {} with given template data for notification request {}. Please check the list of required fields",
          templateId, notificationId);
    }
    String message = messageOpt.get();

    boolean sent = false;
    for (String microsoftTeamsWebhookUrl : microsoftTeamsWebhookUrls) {
      int responseCode = microsoftTeamsSenderServiceImpl.sendMessage(message, microsoftTeamsWebhookUrl);
      sent = sent || (responseCode >= 200 && responseCode < 300);
    }
    log.info(sent ? "Notificaition request {} sent" : "Failed to send notification for request {}", notificationId);
    return sent;
  }

  private Optional<String> getDecoratedNotificationMessage(String templateText, Map<String, String> params) {
    templateText = StrSubstitutor.replace(templateText, params);
    if (placeHolderPattern.matcher(templateText).find()) {
      return Optional.empty();
    }
    return Optional.of(templateText);
  }

  private List<String> getRecipients(NotificationRequest notificationRequest) {
    MSTeam msTeamDetails = notificationRequest.getMsTeam();
    List<String> recipients = new ArrayList<>(msTeamDetails.getMsTeamKeysList());
    List<String> microsoftTeamWebHookUrls = notificationSettingsService.getNotificationSettingsForGroups(
        msTeamDetails.getUserGroupIdsList(), NotificationChannelType.MSTEAMS, notificationRequest.getAccountId());
    recipients.addAll(microsoftTeamWebHookUrls);
    return recipients;
  }

  Map<String, String> processTemplateVariables(Map<String, String> templateVariables) {
    Map<String, String> clonedTemplateVariables = new HashMap<>(templateVariables);
    clonedTemplateVariables.forEach((key, value) -> {
      String newValue = handleSpecialCharacters(key, value);
      if (newValue.isEmpty() && key.endsWith(NAME)) {
        newValue = NA;
      }
      clonedTemplateVariables.put(key, newValue);
    });
    formatTemplateUrlAndName(clonedTemplateVariables);
    String notificationStatus = templateVariables.getOrDefault(KEY_VERB, EMPTY);
    clonedTemplateVariables.put(THEME_COLOR, getThemeColor(notificationStatus, BLUE_COLOR));
    return clonedTemplateVariables;
  }

  private String getThemeColor(String status, String defaultColor) {
    switch (status) {
      case "completed":
        return COMPLETED_COLOR;
      case "expired":
      case "rejected":
      case "failed":
        return FAILED_COLOR;
      case "paused":
        return PAUSED_COLOR;
      case "resumed":
        return RESUMED_COLOR;
      case "aborted":
        return ABORTED_COLOR;
      default:
        return defaultColor;
    }
  }

  String handleSpecialCharacters(String key, String value) {
    if (key.contains(URL)) {
      return value;
    }
    value = value.replaceAll(ASTERISK, ASTERISK_REPLACEMENT).replaceAll(NEW_LINE, NEW_LINE_REPLACEMENT);
    String[] parts = value.split(SPACE);
    for (int index = 0; index < parts.length; index++) {
      String formattedValue = parts[index].replaceAll(UNDERSCORE, UNDERSCORE_REPLACEMENT);
      if (checkIfStringIsValidUrl(parts[index])) {
        parts[index] = format("[%s](%s)", formattedValue, parts[index]);
      } else {
        parts[index] = formattedValue;
      }
    }
    return join(SPACE, parts);
  }

  boolean checkIfStringIsValidUrl(String value) {
    UrlValidator urlValidator = new UrlValidator(schemes);
    return urlValidator.isValid(value);
  }

  private void formatTemplateUrlAndName(Map<String, String> templateVariables) {
    for (String key : TEMPLATE_KEYS_TO_BE_PROCESSED) {
      if (templateVariables.containsKey(key + NAME) && templateVariables.containsKey(key + URL)) {
        if (ARTIFACTS.equals(key)) {
          templateVariables.put(key, templateVariables.get(key + NAME));
        } else {
          String[] names = templateVariables.get(key + NAME).split(COMMA);
          String[] urls = templateVariables.get(key + URL).split(COMMA);
          String updatedValue = getUpdatedValue(names, urls);
          if (PIPELINE.equals(key)) {
            updatedValue = (!NA.equals(updatedValue)) ? format("in pipeline %s", updatedValue) : EMPTY;
          }
          templateVariables.put(key, updatedValue);
        }
      }
    }
  }

  String getUpdatedValue(String[] names, String[] urls) {
    List<String> updatedValue = new ArrayList<>();
    if (names.length != urls.length) {
      log.info("Name and URL array has length mismatch. Names={} Urls={}", names, urls);
    } else {
      for (int index = 0; index < names.length; index++) {
        if (StringUtils.isNotEmpty(urls[index])) {
          updatedValue.add(format("[%s](%s)", names[index], urls[index]));
        } else {
          updatedValue.add(names[index]);
        }
      }
    }
    return join(", ", updatedValue);
  }
}
