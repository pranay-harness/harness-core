package software.wings.service.impl.notifications;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.common.Constants.ABORTED_COLOR;
import static software.wings.common.Constants.COMPLETED_COLOR;
import static software.wings.common.Constants.FAILED_COLOR;
import static software.wings.common.Constants.LINK_COLOR;
import static software.wings.common.Constants.PAUSED_COLOR;
import static software.wings.common.Constants.RESUMED_COLOR;
import static software.wings.common.NotificationMessageResolver.getDecoratedNotificationMessage;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Notification;
import software.wings.common.NotificationMessageResolver;
import software.wings.common.NotificationMessageResolver.ChannelTemplate.EmailTemplate;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.EmailNotificationService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
public class EmailDispatcher {
  private static final Logger log = LoggerFactory.getLogger(EmailDispatcher.class);

  @Inject private NotificationMessageResolver notificationMessageResolver;
  @Inject private EmailNotificationService emailNotificationService;

  public void dispatch(List<Notification> notifications, List<String> toAddress) {
    if (isEmpty(toAddress)) {
      return;
    }

    List<String> validToAddresses = new ArrayList<>();
    for (String emailAddress : toAddress) {
      if (isNotBlank(emailAddress)) {
        validToAddresses.add(emailAddress);
      }
    }

    if (isEmpty(validToAddresses)) {
      return;
    }

    List<String> emailBodyList = new ArrayList<>();
    List<String> emailSubjectList = new ArrayList<>();
    notifications.forEach(notification -> {
      EmailTemplate emailTemplate =
          notificationMessageResolver.getEmailTemplate(notification.getNotificationTemplateId());
      if (emailTemplate == null) {
        log.error("No email template found for templateId {}", notification.getNotificationTemplateId());
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

    EmailData emailData = EmailData.builder().to(validToAddresses).subject(subject).body(body).system(true).build();
    emailData.setRetries(2);
    emailData.setCc(Collections.emptyList());
    emailNotificationService.sendAsync(emailData);
  }

  public static String processEmailHtml(String text) {
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
}
