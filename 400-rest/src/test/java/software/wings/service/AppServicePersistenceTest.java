/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.threading.Morpheus.sleep;

import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.mockChecker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.limits.LimitCheckerFactory;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueListener;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.Alert.AlertKeys;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.ApprovalNeededAlert;
import software.wings.prune.PruneEvent;
import software.wings.rules.SetupScheduler;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppService;

import com.google.inject.Inject;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.quartz.SchedulerException;

@SetupScheduler
public class AppServicePersistenceTest extends WingsBaseTest {
  @Inject private HPersistence persistence;

  @Inject private AlertService alertService;

  @Inject @InjectMocks AppService appService;

  @Inject private QueueListener<PruneEvent> pruneEventQueueListener;

  @Mock private LimitCheckerFactory limitCheckerFactory;

  private static String appId = APP_ID;
  private static String dummyAppID = "dummy" + appId;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldDeleteApplication()
      throws SchedulerException, InterruptedException, ExecutionException, TimeoutException {
    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    assertThat(persistence.get(Application.class, appId)).isNull();

    // Create some other application. We make this to make sure that deleting items that belong to one
    // application does not affect others.
    Application dummyApplication =
        anApplication().uuid(dummyAppID).name("DUMMY_APP_NAME").accountId(ACCOUNT_ID).build();
    appService.save(dummyApplication);

    // Create the test target application
    Application application = anApplication().uuid(appId).name("APP_NAME").accountId(ACCOUNT_ID).build();
    appService.save(application);

    // Make sure that we can obtain the application after we saved it
    assertThat(persistence.get(Application.class, APP_ID)).isNotNull();

    // Add alert to the dummy and the target application
    alertService.openAlert(ACCOUNT_ID, dummyAppID, AlertType.ApprovalNeeded, ApprovalNeededAlert.builder().build())
        .get();
    alertService.openAlert(ACCOUNT_ID, appId, AlertType.ApprovalNeeded, ApprovalNeededAlert.builder().build()).get();

    // Make sure that we added the two alerts
    PageResponse<Alert> alerts =
        alertService.list(aPageRequest().addFilter(AlertKeys.accountId, EQ, ACCOUNT_ID).build());

    assertThat(alerts.size()).isEqualTo(2);

    // TODO: add to the application from all other objects that are owned from application

    // Delete the target application
    appService.delete(APP_ID);

    sleep(PruneEvent.DELAY);

    pruneEventQueueListener.pumpAll();

    // Make sure we cannot access the application after it was deleted
    assertThat(persistence.get(Application.class, APP_ID)).isNull();

    // Make sure that just the alert for the application are deleted
    alerts = alertService.list(aPageRequest().addFilter(AlertKeys.accountId, EQ, ACCOUNT_ID).build());

    assertThat(alerts.size()).isEqualTo(1);
  }
}
