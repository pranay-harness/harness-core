package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.common.Constants.ABORTED_COLOR;
import static software.wings.common.Constants.COMPLETED_COLOR;
import static software.wings.common.Constants.FAILED_COLOR;
import static software.wings.common.Constants.HARNESS_NAME;
import static software.wings.common.Constants.LINK_COLOR;
import static software.wings.common.Constants.PAUSED_COLOR;
import static software.wings.common.Constants.RESUMED_COLOR;
import static software.wings.common.NotificationMessageResolver.getDecoratedNotificationMessage;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Notification;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SlackConfig;
import software.wings.beans.User;
import software.wings.common.NotificationMessageResolver;
import software.wings.common.NotificationMessageResolver.ChannelTemplate.EmailTemplate;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
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
import java.util.Map;
import java.util.Map.Entry;
/**
 * Created by rishi on 10/30/16.
 */
@Singleton
public class NotificationDispatcherServiceImpl implements NotificationDispatcherService {
  private static final Logger logger = LoggerFactory.getLogger(NotificationDispatcherServiceImpl.class);
  @Inject private NotificationSetupService notificationSetupService;
  @Inject private EmailNotificationService emailNotificationService;
  @Inject private SlackNotificationService slackNotificationService;
  @Inject private SettingsService settingsService;
  @Inject private NotificationMessageResolver notificationMessageResolver;
  @Inject private UserService userService;

  @Override
  public void dispatchNotification(Notification notification, List<NotificationRule> notificationRules) {
    if (notificationRules == null) {
      return;
    }
    for (NotificationRule notificationRule : notificationRules) {
      dispatch(singletonList(notification), notificationRule.getNotificationGroups());
    }
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
            .collect(toList());

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
          List<String> toAddresses = users.stream().filter(User::isEmailVerified).map(User::getEmail).collect(toList());
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
        -> channels.forEach(
            channel -> slackNotificationService.sendMessage(slackConfig, channel, HARNESS_NAME, message)));
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
    return text.replaceAll("<<<", "<b><<a>>href=\"")
        .replaceAll("\\|-\\|", "\" target=\"_blank\">")
        .replaceAll(">>>", "</a></b>")
        .replaceAll("<<a>>", "<a style=\"text-decoration:none; color:" + LINK_COLOR + ";\" ")
        .replaceAll("<<top-div>>", "<div style=\"margin-top:12px; margin-left:14px\">")
        .replaceAll("<<bottom-div>>",
            "<div style=\"margin:15px; padding-left:7px; "
                + "border-left-width:3px; border-radius:3px; border-left-style:solid; font-size:small; border-left-color:")
        .replaceAll("<<completed-color>>", COMPLETED_COLOR + ";\">")
        .replaceAll("<<failed-color>>", FAILED_COLOR + ";\">")
        .replaceAll("<<paused-color>>", PAUSED_COLOR + ";\">")
        .replaceAll("<<resumed-color>>", RESUMED_COLOR + ";\">")
        .replaceAll("<<aborted-color>>", ABORTED_COLOR + ";\">")
        .replaceAll("<<rejected-color>>", FAILED_COLOR + ";\">")
        .replaceAll("<<expired-color>>", FAILED_COLOR + ";\">")
        .replaceAll("<<img-path>>", "<div><span><img src=\"https://s3.amazonaws.com/wings-assets/slackicons/")
        .replaceAll("<<img-suffix>>",
            ".png\" height=\"13\" width=\"13\" style=\"padding-right:5px; padding-top:5px;\"></span>"
                + "<span style=\"color:gray; display:inline-block; vertical-align:top; margin-top:4px;\">");
  }

  @Override
  public EmailData obtainEmailData(String notificationTemplateId, Map<String, String> placeholderValues) {
    EmailTemplate emailTemplate = notificationMessageResolver.getEmailTemplate(notificationTemplateId);
    String body =
        notificationMessageResolver.getDecoratedNotificationMessage(emailTemplate.getBody(), placeholderValues);
    String subject =
        notificationMessageResolver.getDecoratedNotificationMessage(emailTemplate.getSubject(), placeholderValues);

    String emailBody = processEmailHtml(body);
    String emailSubject = processEmailHtml(subject);

    return EmailData.builder().subject(emailSubject).body(emailBody).build();
  }
}
