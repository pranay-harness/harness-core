package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.EmbeddedUser;
import io.harness.notifications.NotificationReceiverInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Notification;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.alert.AlertNotificationRule;
import software.wings.beans.security.UserGroup;
import software.wings.common.NotificationMessageResolver;
import software.wings.common.NotificationMessageResolver.ChannelTemplate.EmailTemplate;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.impl.notifications.EmailDispatcher;
import software.wings.service.impl.notifications.NotificationDispatcher;
import software.wings.service.impl.notifications.UseNotificationGroup;
import software.wings.service.impl.notifications.UseUserGroup;
import software.wings.service.intfc.NotificationDispatcherService;
import software.wings.service.intfc.UserGroupService;
import software.wings.utils.Misc;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by rishi on 10/30/16.
 */
@Singleton
public class NotificationDispatcherServiceImpl implements NotificationDispatcherService {
  private static final Logger logger = LoggerFactory.getLogger(NotificationDispatcherServiceImpl.class);
  @Inject private NotificationMessageResolver notificationMessageResolver;
  @Inject private UserGroupService userGroupService;
  @Inject @UseNotificationGroup private NotificationDispatcher<NotificationGroup> notificationGroupDispatcher;
  @Inject @UseUserGroup private NotificationDispatcher<UserGroup> userGroupDispatcher;

  @Inject private EmailDispatcher emailDispatcher;

  @Override
  public void dispatchNotification(Notification notification, List<NotificationRule> notificationRules) {
    if (notificationRules == null) {
      return;
    }

    String accountId = notification.getAccountId();
    if (StringUtils.isEmpty(accountId)) {
      throw new IllegalStateException("No AccountId present in notification. Notification: " + notification);
    }

    for (NotificationRule notificationRule : notificationRules) {
      List<UserGroup> userGroups = notificationRule.getUserGroupIds()
                                       .stream()
                                       .distinct()
                                       .map(id -> userGroupService.get(accountId, id))
                                       .filter(Objects::nonNull)
                                       .collect(toList());

      dispatch(singletonList(notification), userGroups);

      // TODO(jatin): delete this once (userGroup -> notificationGroup) Migration is done
      dispatch(singletonList(notification), notificationRule.getNotificationGroups());
    }
  }

  @Override
  public void dispatch(Notification notification, List<AlertNotificationRule> rules) {
    String accountId = notification.getAccountId();
    if (StringUtils.isEmpty(accountId)) {
      throw new IllegalStateException(
          "[dispatch-alertNotificationRule] No AccountId present in notification. Notification: " + notification);
    }

    for (AlertNotificationRule rule : rules) {
      List<UserGroup> userGroups = rule.getUserGroupsToNotify()
                                       .stream()
                                       .distinct()
                                       .map(id -> userGroupService.get(accountId, id))
                                       .filter(Objects::nonNull)
                                       .collect(toList());

      dispatch(singletonList(notification), userGroups);
    }
  }

  private <T extends NotificationReceiverInfo> void dispatch(
      List<Notification> notifications, final List<T> notificationReceivers) {
    List<T> receivers = notificationReceivers.stream().filter(Objects::nonNull).collect(toList());

    if (isEmpty(receivers) || isEmpty(notifications)) {
      return;
    }

    for (NotificationReceiverInfo notificationReceiver : receivers) {
      if (notificationReceiver instanceof NotificationGroup) {
        logger.info("[notification-group-dispatch] accountId={}", notifications.get(0).getAccountId());
        notificationGroupDispatcher.dispatch(notifications, (NotificationGroup) notificationReceiver);
      } else if (notificationReceiver instanceof UserGroup) {
        userGroupDispatcher.dispatch(notifications, (UserGroup) notificationReceiver);
      } else {
        logger.error("Unhandled implementation of NotificationReceiverInfo. Class: {}",
            notificationReceiver.getClass().getSimpleName());
      }
    }
  }

  @Override
  public EmailData obtainEmailData(String notificationTemplateId, Map<String, String> placeholderValues) {
    EmailTemplate emailTemplate = notificationMessageResolver.getEmailTemplate(notificationTemplateId);
    String body =
        notificationMessageResolver.getDecoratedNotificationMessage(emailTemplate.getBody(), placeholderValues);
    String subject =
        notificationMessageResolver.getDecoratedNotificationMessage(emailTemplate.getSubject(), placeholderValues);

    String emailBody = EmailDispatcher.processEmailHtml(body);
    String emailSubject = EmailDispatcher.processEmailHtml(subject);

    return EmailData.builder().subject(emailSubject).body(emailBody).build();
  }

  @Override
  public void dispatchNotificationToTriggeredByUserOnly(List<Notification> notifications, EmbeddedUser user) {
    try {
      emailDispatcher.dispatch(notifications, asList(user.getEmail()));
    } catch (Exception e) {
      logger.warn(Misc.getMessage(e));
    }
  }
}
