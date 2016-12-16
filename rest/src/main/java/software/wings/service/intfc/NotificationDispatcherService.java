package software.wings.service.intfc;

import software.wings.beans.Notification;
import software.wings.beans.NotificationRule;

import java.util.List;

/**
 * Created by rishi on 10/30/16.
 */
public interface NotificationDispatcherService {
  void dispatchNotification(Notification notification);
  void dispatchNotification(Notification notification, List<NotificationRule> notificationRuleList);
}
