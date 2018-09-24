package software.wings.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.inject.Inject;

import io.harness.beans.PageResponse;
import org.junit.Test;
import org.quartz.SchedulerException;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.ApprovalNeededAlert;
import software.wings.dl.WingsPersistence;
import software.wings.rules.SetupScheduler;
import software.wings.scheduler.JobScheduler;
import software.wings.scheduler.PruneEntityJob;
import software.wings.scheduler.TestJobListener;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppService;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@SetupScheduler
public class AppServicePersistenceTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  @Inject private AlertService alertService;

  @Inject AppService appService;

  @Inject private JobScheduler jobScheduler;

  private static String appId = APP_ID;
  private static String dummyAppID = "dummy" + appId;

  @Test
  public void shouldDeleteApplication()
      throws SchedulerException, InterruptedException, ExecutionException, TimeoutException {
    assertThat(wingsPersistence.get(Application.class, appId)).isNull();

    // Create some other application. We make this to make sure that deleting items that belong to one
    // application does not affect others.
    Application dummyApplication =
        anApplication().withUuid(dummyAppID).withName("DUMMY_APP_NAME").withAccountId(ACCOUNT_ID).build();
    appService.save(dummyApplication);

    // Create the test target application
    Application application = anApplication().withUuid(appId).withName("APP_NAME").withAccountId(ACCOUNT_ID).build();
    appService.save(application);

    // Make sure that we can obtain the application after we saved it
    assertThat(wingsPersistence.get(Application.class, APP_ID)).isNotNull();

    // Add alert to the dummy and the target application
    alertService.openAlert(ACCOUNT_ID, dummyAppID, AlertType.ApprovalNeeded, ApprovalNeededAlert.builder().build())
        .get();
    alertService.openAlert(ACCOUNT_ID, appId, AlertType.ApprovalNeeded, ApprovalNeededAlert.builder().build()).get();

    // Make sure that we added the two alerts
    PageResponse<Alert> alerts =
        alertService.list(aPageRequest().addFilter(Alert.ACCOUNT_ID_KEY, EQ, ACCOUNT_ID).build());

    assertThat(alerts.size()).isEqualTo(2);

    // TODO: add to the application from all other objects that are owned from application

    TestJobListener listener = new TestJobListener(PruneEntityJob.GROUP + "." + APP_ID);
    jobScheduler.getScheduler().getListenerManager().addJobListener(listener);

    // Delete the target application
    appService.delete(APP_ID);

    // Make sure we cannot access the application after it was deleted
    assertThat(wingsPersistence.get(Application.class, APP_ID)).isNull();

    listener.waitToSatisfy(ofSeconds(60 + 10));

    // Make sure that just the alert for the application are deleted
    alerts = alertService.list(aPageRequest().addFilter(Alert.ACCOUNT_ID_KEY, EQ, ACCOUNT_ID).build());

    assertThat(alerts.size()).isEqualTo(1);
  }
}
