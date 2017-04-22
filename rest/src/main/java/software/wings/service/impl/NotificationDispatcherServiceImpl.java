package software.wings.service.impl;

import static java.util.Arrays.asList;
import static software.wings.beans.ExecutionScope.WORKFLOW;
import static software.wings.beans.ExecutionScope.WORKFLOW_PHASE;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.WORKFLOW_FAILED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.WORKFLOW_PHASE_FAILED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.WORKFLOW_PHASE_SUCCESSFUL_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.WORKFLOW_SUCCESSFUL_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.getDecoratedNotificationMessage;
import static software.wings.helpers.ext.mail.EmailData.Builder.anEmailData;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ExecutionScope;
import software.wings.beans.Notification;
import software.wings.beans.NotificationBatch;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SlackConfig;
import software.wings.common.NotificationMessageResolver;
import software.wings.common.NotificationMessageResolver.ChannelTemplate.EmailTemplate;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.NotificationDispatcherService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SlackNotificationService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Created by rishi on 10/30/16.
 */
@Singleton
public class NotificationDispatcherServiceImpl implements NotificationDispatcherService {
  private static final ImmutableMap<ExecutionScope, List<NotificationMessageType>> BATCH_END_TEMPLATES =
      ImmutableMap.of(WORKFLOW, asList(WORKFLOW_FAILED_NOTIFICATION, WORKFLOW_SUCCESSFUL_NOTIFICATION), WORKFLOW_PHASE,
          asList(WORKFLOW_PHASE_FAILED_NOTIFICATION, WORKFLOW_PHASE_SUCCESSFUL_NOTIFICATION));
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private NotificationSetupService notificationSetupService;
  @Inject private EmailNotificationService emailNotificationService;
  @Inject private SlackNotificationService slackNotificationService;
  @Inject private SettingsService settingsService;
  @Inject private NotificationMessageResolver notificationMessageResolver;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void dispatchNotification(Notification notification, List<NotificationRule> notificationRules) {
    if (notificationRules == null) {
      return;
    }
    for (NotificationRule notificationRule : notificationRules) {
      if (notificationRule.isBatchNotifications()) {
        batchDispatch(notification, notificationRule);
      } else {
        dispatch(asList(notification), notificationRule.getNotificationGroups());
      }
    }
  }

  private void batchDispatch(Notification notification, NotificationRule notificationRule) {
    String batchId = String.join("-", notificationRule.getUuid(), notification.getEntityId());

    Query<NotificationBatch> query = wingsPersistence.createQuery(NotificationBatch.class)
                                         .field("appId")
                                         .equal(notification.getAppId())
                                         .field("batchId")
                                         .equal(batchId);
    UpdateOperations<NotificationBatch> updateOperations =
        wingsPersistence.createUpdateOperations(NotificationBatch.class)
            .set("batchId", batchId)
            .set("notificationRule", notificationRule)
            .addToSet("notifications", notification);

    NotificationBatch notificationBatch = wingsPersistence.upsert(query, updateOperations);

    if (isLastNotificationInBatch(notification, notificationRule)) {
      dispatch(notificationBatch.getNotifications(), notificationBatch.getNotificationRule().getNotificationGroups());
      wingsPersistence.delete(notificationBatch);
    }
  }

  private boolean isLastNotificationInBatch(Notification notification, NotificationRule notificationRule) {
    // TODO:: revisit. not sure if this logic belongs here.
    return BATCH_END_TEMPLATES.get(notificationRule.getExecutionScope())
        .contains(NotificationMessageType.valueOf(notification.getNotificationTemplateId()));
  }

  private void dispatch(List<Notification> notifications, List<NotificationGroup> notificationGroups) {
    if (notificationGroups == null || notifications == null || notifications.size() == 0) {
      return;
    }
    String appId = notifications.get(0).getAppId();
    notificationGroups =
        notificationGroups.stream()
            .map(
                notificationGroup -> notificationSetupService.readNotificationGroup(appId, notificationGroup.getUuid()))
            .filter(notificationGroup -> notificationGroup.getAddressesByChannelType() != null)
            .collect(Collectors.toList());

    for (NotificationGroup notificationGroup : notificationGroups) {
      for (Entry<NotificationChannelType, List<String>> entry :
          notificationGroup.getAddressesByChannelType().entrySet()) {
        if (entry.getKey() == NotificationChannelType.EMAIL) {
          dispatchEmail(notifications, entry.getValue());
        }
        if (entry.getKey() == NotificationChannelType.SLACK) {
          dispatchSlackMessage(notifications, entry.getValue());
        }
      }
    }
  }

  private void dispatchSlackMessage(List<Notification> notifications, List<String> channels) {
    if (channels == null || channels.size() == 0) {
      return;
    }

    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(
        notifications.get(0).getAccountId(), SettingVariableTypes.SLACK.name());
    if (settingAttributes == null || settingAttributes.size() == 0) {
      logger.error("No slack configuration found ");
      return;
    }
    SettingAttribute settingAttribute = settingAttributes.iterator().next();
    SlackConfig slackConfig = (SlackConfig) settingAttribute.getValue();

    List<String> messages = new ArrayList<>();

    notifications.forEach(notification -> {
      String slackTemplate = notificationMessageResolver.getSlackTemplate(notification.getNotificationTemplateId());
      if (slackTemplate == null) {
        logger.error("No slack template found for templateId {}", notification.getNotificationTemplateId());
        return;
      }
      messages.add(getDecoratedNotificationMessage(slackTemplate, notification.getNotificationTemplateVariables()));
    });

    String concatenatedMessage = String.join("\n\n", messages);
    channels.forEach(channel
        -> slackNotificationService.sendMessage(slackConfig, channel, "Wings Notification Bot", concatenatedMessage));
  }

  private void dispatchEmail(List<Notification> notifications, List<String> toAddress) {
    if (toAddress == null || toAddress.size() == 0) {
      return;
    }

    List<String> emailBodyList = new ArrayList<>();
    List<String> emailSubjectList = new ArrayList<>();
    notifications.forEach(notification -> {
      EmailTemplate emailTemplate =
          notificationMessageResolver.getEmailTemplate(notification.getNotificationTemplateId());
      if (emailTemplate == null) {
        logger.error("No email template found for templateId {}", notification.getNotificationTemplateId());
        return;
      }
      emailBodyList.add(
          getDecoratedNotificationMessage(emailTemplate.getBody(), notification.getNotificationTemplateVariables()));
      emailSubjectList.add(
          getDecoratedNotificationMessage(emailTemplate.getSubject(), notification.getNotificationTemplateVariables()));
    });

    if (emailBodyList.size() == 0 || emailSubjectList.size() == 0) {
      return;
    }

    String body = String.join("\n\n", emailBodyList);
    String subject = emailSubjectList.get(emailSubjectList.size() - 1);

    emailNotificationService.sendAsync(
        anEmailData().withTo(toAddress).withRetries(2).withSubject(subject).withBody(body).withSystem(true).build());
  }
}
