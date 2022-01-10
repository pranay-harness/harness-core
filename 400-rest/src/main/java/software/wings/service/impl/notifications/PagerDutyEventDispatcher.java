/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.notifications;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.model.EventType;

import software.wings.beans.Notification;
import software.wings.common.NotificationMessageResolver;
import software.wings.common.NotificationMessageResolver.PagerDutyTemplate;
import software.wings.features.PagerDutyNotificationFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.service.intfc.pagerduty.PagerDutyService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class PagerDutyEventDispatcher {
  @Inject PagerDutyService pagerDutyService;
  @Inject private NotificationMessageResolver notificationMessageResolver;
  @Inject @Named(PagerDutyNotificationFeature.FEATURE_NAME) private PremiumFeature pagerDutyNotificationFeature;

  public void dispatch(String accountId, List<Notification> notifications, String pagerDutyKey) {
    if (!pagerDutyNotificationFeature.isAvailableForAccount(accountId)) {
      log.info("PagerDuty notification will be ignored since it's an unavailable feature for accountId={}", accountId);
      return;
    }

    if (isEmpty(notifications)) {
      return;
    }

    for (Notification notification : notifications) {
      if (EventType.CLOSE_ALERT == notification.getEventType()) {
        log.info("ignoring close alert for pager duty {}", notification);
        continue;
      }
      PagerDutyTemplate pagerDutySummaryTemplate =
          notificationMessageResolver.getPagerDutyTemplate(notification.getNotificationTemplateId());
      if (pagerDutySummaryTemplate == null) {
        log.error("No pagerDuty template found for templateId {}", notification.getNotificationTemplateId());
        return;
      }
      pagerDutyService.sendPagerDutyEvent(notification, pagerDutyKey, pagerDutySummaryTemplate);
    }
  }
}
