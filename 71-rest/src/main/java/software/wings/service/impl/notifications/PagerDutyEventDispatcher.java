package software.wings.service.impl.notifications;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Notification;
import software.wings.common.NotificationMessageResolver;
import software.wings.common.NotificationMessageResolver.PagerDutyTemplate;
import software.wings.features.PagerDutyNotificationFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.service.intfc.pagerduty.PagerDutyService;

import java.util.List;

@Singleton
@Slf4j
public class PagerDutyEventDispatcher {
  @Inject PagerDutyService pagerDutyService;
  @Inject private NotificationMessageResolver notificationMessageResolver;
  @Inject @Named(PagerDutyNotificationFeature.FEATURE_NAME) private PremiumFeature pagerDutyNotificationFeature;

  public void dispatch(String accountId, List<Notification> notifications, String pagerDutyKey) {
    if (!pagerDutyNotificationFeature.isAvailableForAccount(accountId)) {
      logger.info(
          "PagerDuty notification will be ignored since it's an unavailable feature for accountId={}", accountId);
      return;
    }

    if (isEmpty(notifications)) {
      return;
    }

    for (Notification notification : notifications) {
      PagerDutyTemplate pagerDutySummaryTemplate =
          notificationMessageResolver.getPagerDutyTemplate(notification.getNotificationTemplateId());
      if (pagerDutySummaryTemplate == null) {
        logger.error("No pagerDuty template found for templateId {}", notification.getNotificationTemplateId());
        return;
      }
      pagerDutyService.sendPagerDutyEvent(notification, pagerDutyKey, pagerDutySummaryTemplate);
    }
  }
}
