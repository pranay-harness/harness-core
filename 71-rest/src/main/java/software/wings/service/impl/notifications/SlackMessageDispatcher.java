package software.wings.service.impl.notifications;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static org.apache.commons.lang3.StringUtils.stripToEmpty;
import static software.wings.common.Constants.HARNESS_NAME;
import static software.wings.common.NotificationMessageResolver.getDecoratedNotificationMessage;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Notification;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SlackConfig;
import software.wings.beans.notification.SlackNotificationConfiguration;
import software.wings.common.NotificationMessageResolver;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SlackNotificationService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class SlackMessageDispatcher {
  private static final Logger log = LoggerFactory.getLogger(SlackMessageDispatcher.class);

  @Inject private SettingsService settingsService;
  @Inject private NotificationMessageResolver notificationMessageResolver;
  @Inject private SlackNotificationService slackNotificationService;

  /**
   * This method is a bit deceiving. It just picks first slack config from DB and notifies that.
   * Also, Slack does NOT let sender specify which channel to notify. That is tied on the webHook URL, and defined when
   * that URL is created. So, {@code channels} param is a bit redundant.
   *
   * The method has been kept for backward compatibility reasons. Prefer to use {@link #dispatch(List,
   * SlackNotificationConfiguration)}.
   */
  @Deprecated
  public void dispatch(List<Notification> notifications, List<String> channels) {
    if (isEmpty(channels)) {
      return;
    }

    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(
        notifications.get(0).getAccountId(), SettingVariableTypes.SLACK.name());
    if (isEmpty(settingAttributes)) {
      log.warn("No slack configuration found ");
      return;
    }
    SettingAttribute settingAttribute = settingAttributes.iterator().next();
    SlackConfig slackConfig = (SlackConfig) settingAttribute.getValue();

    List<String> messages = new ArrayList<>();

    notifications.forEach(notification -> {
      String slackTemplate = notificationMessageResolver.getSlackTemplate(notification.getNotificationTemplateId());
      if (slackTemplate == null) {
        log.error("No slack template found for templateId {}", notification.getNotificationTemplateId());
        return;
      }
      messages.add(getDecoratedNotificationMessage(slackTemplate, notification.getNotificationTemplateVariables()));
    });

    messages.forEach(message
        -> channels.forEach(
            channel -> slackNotificationService.sendMessage(slackConfig, channel, HARNESS_NAME, message)));
  }

  public void dispatch(List<Notification> notifications, SlackNotificationConfiguration slackConfig) {
    if (isEmpty(notifications)) {
      return;
    }

    List<String> messages = new ArrayList<>();

    notifications.forEach(notification -> {
      String slackTemplate = notificationMessageResolver.getSlackTemplate(notification.getNotificationTemplateId());
      if (slackTemplate == null) {
        log.error("No slack template found for templateId {}", notification.getNotificationTemplateId());
        return;
      }
      messages.add(getDecoratedNotificationMessage(slackTemplate, notification.getNotificationTemplateVariables()));
    });

    for (String message : messages) {
      slackNotificationService.sendMessage(slackConfig, stripToEmpty(slackConfig.getName()), HARNESS_NAME, message);
    }
  }
}
