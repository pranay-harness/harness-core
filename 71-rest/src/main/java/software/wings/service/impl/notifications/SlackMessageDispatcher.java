package software.wings.service.impl.notifications;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.stripToEmpty;
import static software.wings.common.Constants.HARNESS_NAME;
import static software.wings.common.NotificationMessageResolver.getDecoratedNotificationMessage;

import com.google.api.client.util.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.FeatureName;
import software.wings.beans.Notification;
import software.wings.beans.SettingAttribute;
import software.wings.beans.notification.SlackNotificationConfiguration;
import software.wings.common.NotificationMessageResolver;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SlackNotificationService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
public class SlackMessageDispatcher {
  private static final Logger log = LoggerFactory.getLogger(SlackMessageDispatcher.class);

  @Inject private SettingsService settingsService;
  @Inject private NotificationMessageResolver notificationMessageResolver;
  @Inject private SlackNotificationService slackNotificationService;
  @Inject private FeatureFlagService featureFlagService;

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

    String accountId = notifications.get(0).getAccountId();
    List<SettingAttribute> settingAttributes =
        settingsService.getGlobalSettingAttributesByType(accountId, SettingVariableTypes.SLACK.name());
    if (isEmpty(settingAttributes)) {
      log.warn("No slack configuration found ");
      return;
    }
    SettingAttribute settingAttribute = settingAttributes.iterator().next();
    SlackNotificationConfiguration slackConfig = (SlackNotificationConfiguration) settingAttribute.getValue();

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
            channel -> slackNotificationService.sendMessage(slackConfig, channel, HARNESS_NAME, message, accountId)));
  }

  public void dispatch(List<Notification> notifications, SlackNotificationConfiguration slackConfig) {
    if (isEmpty(notifications)) {
      return;
    }

    String accountId = notifications.get(0).getAccountId();

    List<String> messages = new ArrayList<>();

    boolean slackMessageThroughDelegate =
        featureFlagService.isEnabled(FeatureName.SEND_SLACK_NOTIFICATION_FROM_DELEGATE, accountId);

    for (Notification notification : notifications) {
      if (notification.getNotificationTemplateVariables().containsKey(SlackApprovalMessageKeys.MESSAGE_IDENTIFIER)
          && !slackMessageThroughDelegate) {
        boolean isChannelNameEmpty = true;
        // Fetch Channel and add to Notification Template Variables
        String slackChannel = stripToEmpty(slackConfig.getName());
        if (isNotEmpty(slackChannel)) {
          isChannelNameEmpty = false;
          if (slackChannel.charAt(0) != '#') {
            slackChannel = "#" + slackChannel;
          }
          notification.getNotificationTemplateVariables().put("channelName", slackChannel);
        }
        // Enable this feature flag to allow user to approve via slack
        boolean slackApprovalsFeatureFlag = featureFlagService.isEnabled(FeatureName.SLACK_APPROVALS, accountId);

        URL url;
        if (slackApprovalsFeatureFlag) {
          if (isChannelNameEmpty) {
            url = this.getClass().getResource(
                SlackApprovalMessageKeys.APPROVAL_MESSAGE_WITHOUT_CHANNEL_NAME_PAYLOAD_TEMPLATE);
          } else {
            url = this.getClass().getResource(SlackApprovalMessageKeys.APPROVAL_MESSAGE_PAYLOAD_TEMPLATE);
          }
        } else {
          if (isChannelNameEmpty) {
            url = this.getClass().getResource(
                SlackApprovalMessageKeys.APPROVAL_MESSAGE_WITHOUT_BUTTONS_WITHOUT_CHANNEL_NAME_PAYLOAD_TEMPLATE);
          } else {
            url =
                this.getClass().getResource(SlackApprovalMessageKeys.APPROVAL_MESSAGE_WITHOUT_BUTTONS_PAYLOAD_TEMPLATE);
          }
        }

        String approvalTemplate;
        StrSubstitutor sub = new StrSubstitutor(notification.getNotificationTemplateVariables());
        try {
          approvalTemplate = Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
          return;
        }
        String resolvedString = sub.replace(approvalTemplate);
        slackNotificationService.sendJSONMessage(
            resolvedString, Collections.singletonList(slackConfig.getOutgoingWebhookUrl()));
        continue;
      }

      String slackTemplate = notificationMessageResolver.getSlackTemplate(notification.getNotificationTemplateId());
      if (slackTemplate == null) {
        log.error("No slack template found for templateId {}", notification.getNotificationTemplateId());
        continue;
      }
      messages.add(getDecoratedNotificationMessage(slackTemplate, notification.getNotificationTemplateVariables()));
    }

    for (String message : messages) {
      slackNotificationService.sendMessage(
          slackConfig, stripToEmpty(slackConfig.getName()), HARNESS_NAME, message, accountId);
    }
  }
}