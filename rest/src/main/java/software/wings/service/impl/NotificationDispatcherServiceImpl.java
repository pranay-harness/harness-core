package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.Arrays.asList;
import static software.wings.beans.ExecutionScope.WORKFLOW;
import static software.wings.beans.ExecutionScope.WORKFLOW_PHASE;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.common.Constants.ABORTED_COLOR;
import static software.wings.common.Constants.COMPLETED_COLOR;
import static software.wings.common.Constants.FAILED_COLOR;
import static software.wings.common.Constants.PAUSED_COLOR;
import static software.wings.common.Constants.RESUMED_COLOR;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.WORKFLOW_FAILED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.WORKFLOW_PHASE_FAILED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.WORKFLOW_PHASE_SUCCESSFUL_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.WORKFLOW_SUCCESSFUL_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.getDecoratedNotificationMessage;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
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
import software.wings.beans.User;
import software.wings.common.NotificationMessageResolver;
import software.wings.common.NotificationMessageResolver.ChannelTemplate.EmailTemplate;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.NotificationDispatcherService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SlackNotificationService;
import software.wings.service.intfc.UserService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
/**
 * Created by rishi on 10/30/16.
 */
@Singleton
public class NotificationDispatcherServiceImpl implements NotificationDispatcherService {
  private static final ImmutableMap<ExecutionScope, List<NotificationMessageType>> BATCH_END_TEMPLATES =
      ImmutableMap.of(WORKFLOW, asList(WORKFLOW_FAILED_NOTIFICATION, WORKFLOW_SUCCESSFUL_NOTIFICATION), WORKFLOW_PHASE,
          asList(WORKFLOW_PHASE_FAILED_NOTIFICATION, WORKFLOW_PHASE_SUCCESSFUL_NOTIFICATION));
  private static final Logger logger = LoggerFactory.getLogger(NotificationDispatcherServiceImpl.class);
  @Inject private NotificationSetupService notificationSetupService;
  @Inject private EmailNotificationService emailNotificationService;
  @Inject private SlackNotificationService slackNotificationService;
  @Inject private SettingsService settingsService;
  @Inject private NotificationMessageResolver notificationMessageResolver;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private UserService userService;

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
    if (notificationGroups == null || isEmpty(notifications)) {
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
      if (notificationGroup.getRoles() != null) {
        // Then collect all the email ids and send for the verified email addresses
        notificationGroup.getRoles().forEach(role -> {
          PageRequest<User> request = aPageRequest()
                                          .withLimit(PageRequest.UNLIMITED)
                                          .addFilter("appId", EQ, notificationGroup.getAppId())
                                          .addFilter("roles", IN, role)
                                          .addFieldsIncluded("email", "emailVerified")
                                          .build();
          PageResponse<User> users = userService.list(request);
          List<String> toAddresses =
              users.stream().filter(User::isEmailVerified).map(User::getEmail).collect(Collectors.toList());
          logger.info("Dispatching notifications to all the users of role {}", role.getRoleType().getDisplayName());
          dispatchEmail(notifications, toAddresses);
        });
      }
      for (Entry<NotificationChannelType, List<String>> entry :
          notificationGroup.getAddressesByChannelType().entrySet()) {
        if (entry.getKey() == NotificationChannelType.EMAIL) {
          try {
            dispatchEmail(notifications, entry.getValue());
          } catch (Exception e) {
            logger.warn(Misc.getMessage(e));
          }
        }
        if (entry.getKey() == NotificationChannelType.SLACK) {
          try {
            dispatchSlackMessage(notifications, entry.getValue());
          } catch (Exception e) {
            logger.warn(Misc.getMessage(e));
          }
        }
      }
    }
  }

  private void dispatchSlackMessage(List<Notification> notifications, List<String> channels) {
    if (isEmpty(channels)) {
      return;
    }

    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(
        notifications.get(0).getAccountId(), SettingVariableTypes.SLACK.name());
    if (isEmpty(settingAttributes)) {
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

    messages.forEach(message
        -> channels.forEach(channel -> slackNotificationService.sendMessage(slackConfig, channel, "harness", message)));
  }

  private void dispatchEmail(List<Notification> notifications, List<String> toAddress) {
    if (isEmpty(toAddress)) {
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

    if (emailBodyList.isEmpty() || emailSubjectList.isEmpty()) {
      return;
    }

    String body = processEmailHtml(String.join("<br>", emailBodyList));
    String subject = emailSubjectList.get(emailSubjectList.size() - 1);

    EmailData emailData = EmailData.builder().to(toAddress).subject(subject).body(body).system(true).build();
    emailData.setRetries(2);
    emailData.setCc(Collections.emptyList());
    emailNotificationService.sendAsync(emailData);
  }

  private String processEmailHtml(String text) {
    return text.replaceAll("<<<", "<b><a href=\"")
        .replaceAll("\\|-\\|", "\" target=\"_blank\">")
        .replaceAll(">>>", "</a></b>")
        .replaceAll("<<top-div>>", "<div style=\"margin-top:12px; margin-left:14px\">")
        .replaceAll("<<bottom-div>>",
            "<div style=\"margin:15px; padding-left:7px; "
                + "border-left-width:3px; border-radius:3px; border-left-style:solid; font-size:small; border-left-color:")
        .replaceAll("<<completed-color>>", COMPLETED_COLOR + ";\">")
        .replaceAll("<<failed-color>>", FAILED_COLOR + ";\">")
        .replaceAll("<<paused-color>>", PAUSED_COLOR + ";\">")
        .replaceAll("<<resumed-color>>", RESUMED_COLOR + ";\">")
        .replaceAll("<<aborted-color>>", ABORTED_COLOR + ";\">")
        .replaceAll("<<img-path>>", "<div><span><img src=\"https://api.harness.io/storage/wings-assets/slackicons/")
        .replaceAll("<<img-suffix>>",
            ".png\" height=\"13\" width=\"13\" style=\"padding-right:5px; padding-top:5px;\"></span>"
                + "<span style=\"color:gray; font-size:small; display:inline-block; vertical-align:top; margin-top:4px;\">");
  }
}
