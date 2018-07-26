package software.wings.service.intfc;

import software.wings.beans.Notification;
import software.wings.beans.NotificationRule;
import software.wings.helpers.ext.mail.EmailData;

import java.util.List;
import java.util.Map;

/**
 * Created by rishi on 10/30/16.
 */
public interface NotificationDispatcherService {
  void dispatchNotification(Notification notification, List<NotificationRule> notificationRuleList);

  EmailData obtainEmailData(String notificationTemplateId, Map<String, String> placeholderValues);
}
