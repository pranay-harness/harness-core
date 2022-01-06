/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.notifications;

import static io.harness.rule.OwnerRule.MOUNIK;

import static software.wings.beans.EntityType.ORCHESTRATED_DEPLOYMENT;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.ENTITY_CREATE_NOTIFICATION;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.InformationNotification;
import software.wings.beans.Notification;
import software.wings.common.NotificationMessageResolver;
import software.wings.service.intfc.pagerduty.PagerDutyService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

@OwnedBy(HarnessTeam.CDC)
public class PagerDutyEventDispatcherTest extends WingsBaseTest {
  @Inject @InjectMocks private PagerDutyEventDispatcher pagerDutyEventDispatcher;
  @Mock private NotificationMessageResolver notificationMessageResolver;
  @Mock private PagerDutyService pagerDutyService;

  @Test
  @Owner(developers = MOUNIK)
  @Category(UnitTests.class)
  public void testPagerDuty() {
    InformationNotification notification = InformationNotification.builder()
                                               .accountId(ACCOUNT_ID)
                                               .appId(APP_ID)
                                               .entityId(WORKFLOW_EXECUTION_ID)
                                               .entityType(ORCHESTRATED_DEPLOYMENT)
                                               .notificationTemplateId(ENTITY_CREATE_NOTIFICATION.name())
                                               .notificationTemplateVariables(ImmutableMap.of("WORKFLOW_NAME",
                                                   WORKFLOW_NAME, "ENV_NAME", ENV_NAME, "DATE", "DATE"))
                                               .build();
    List<Notification> notifications = Collections.singletonList(notification);
    NotificationMessageResolver.PagerDutyTemplate pagerDutyTemplate =
        new NotificationMessageResolver.PagerDutyTemplate();
    when(notificationMessageResolver.getPagerDutyTemplate(ENTITY_CREATE_NOTIFICATION.name()))
        .thenReturn(pagerDutyTemplate);
    pagerDutyEventDispatcher.dispatch(ACCOUNT_ID, notifications, "Key");

    Mockito.verify(pagerDutyService).sendPagerDutyEvent(Mockito.any(), Mockito.any(), Mockito.any());
  }
}
