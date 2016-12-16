package software.wings.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Notification;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.NotificationDispatcherService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.NotificationSetupService;

import java.util.List;
import java.util.Map.Entry;
import javax.inject.Inject;

/**
 * Created by rishi on 10/30/16.
 */
public class NotificationDispatcherServiceImpl implements NotificationDispatcherService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private NotificationSetupService notificationSetupService;
  @Inject private EmailNotificationService<EmailData> emailNotificationService;
  @Inject private NotificationService notificationService;

  @Override
  public void dispatchNotification(Notification notification) {
    List<NotificationRule> notificationRules = notificationSetupService.listNotificationRules(notification.getAppId());
    if (notificationRules == null || notificationRules.isEmpty()) {
      logger.debug("No notification rule found for the appId: {}.. skipping dispatch for notificationID: {}",
          notification.getAppId(), notification.getUuid());
      return;
    }

    dispatchNotification(notification, notificationRules);
  }

  @Override
  public void dispatchNotification(Notification notification, List<NotificationRule> notificationRules) {
    // TODO: match the rule based on filter

    List<NotificationRule> matchingRules = notificationRules;
    for (NotificationRule notificationRule : matchingRules) {
      logger.debug("Processing notificationRule: {}", notificationRule.getUuid());
      dispatch(notification, notificationRule.getNotificationGroups());
    }
  }

  private void dispatch(Notification notification, List<NotificationGroup> notificationGroups) {
    if (notificationGroups == null) {
      return;
    }

    for (NotificationGroup notificationGroup : notificationGroups) {
      if (notificationGroup.getAddressesByChannelType() == null) {
        continue;
      }
      for (Entry<NotificationChannelType, List<String>> entry :
          notificationGroup.getAddressesByChannelType().entrySet()) {
        if (entry.getKey() == NotificationChannelType.EMAIL) {
          dispatchEmail(notification, entry.getValue());
        }
        // TODO: handle more options other than email.
      }
    }
  }

  private void dispatchEmail(Notification notification, List<String> toAddress) {
    // TODO: determine the right template for the notification
    emailNotificationService.sendAsync(toAddress, null, notification.getUuid(), notification.getUuid());
  }
}
