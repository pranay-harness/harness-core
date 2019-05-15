package software.wings.service.intfc.pagerduty;

import software.wings.beans.Notification;
import software.wings.common.NotificationMessageResolver.PagerDutyTemplate;

public interface PagerDutyService {
  boolean validateKey(String pagerDutyKey);
  boolean sendPagerDutyEvent(Notification notification, String pagerDutyKey, PagerDutyTemplate template);
}
