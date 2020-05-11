package software.wings.service.intfc.pagerduty;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.Notification;
import software.wings.common.NotificationMessageResolver.PagerDutyTemplate;

@OwnedBy(CDC)
public interface PagerDutyService {
  boolean validateKey(String pagerDutyKey);
  boolean validateCreateTestEvent(String pagerDutyKey);
  boolean sendPagerDutyEvent(Notification notification, String pagerDutyKey, PagerDutyTemplate template);
}
