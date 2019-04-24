package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.alerts.AlertStatus.Closed;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.alert.AlertType.ApprovalNeeded;
import static software.wings.beans.alert.AlertType.ManualInterventionNeeded;
import static software.wings.beans.alert.AlertType.NoActiveDelegates;
import static software.wings.beans.alert.AlertType.NoEligibleDelegates;
import static software.wings.beans.alert.NoEligibleDelegatesAlert.NoEligibleDelegatesAlertBuilder.aNoEligibleDelegatesAlert;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.event.model.Event;
import io.harness.event.publisher.EventPublisher;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import software.wings.WingsBaseTest;
import software.wings.beans.TaskGroup;
import software.wings.beans.TaskType;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertData;
import software.wings.beans.alert.ApprovalNeededAlert;
import software.wings.beans.alert.ManualInterventionNeededAlert;
import software.wings.beans.alert.NoActiveDelegatesAlert;
import software.wings.beans.alert.NoEligibleDelegatesAlert;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AssignDelegateService;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class AlertServiceTest extends WingsBaseTest {
  @Mock private ExecutorService executorService;
  @Mock private AssignDelegateService assignDelegateService;

  @Inject @InjectMocks private AlertService alertService;

  @Mock private EventPublisher eventPublisher;

  @Inject private WingsPersistence wingsPersistence;

  private static Answer executeRunnable(ArgumentCaptor<Runnable> runnableCaptor) {
    return invocation -> {
      runnableCaptor.getValue().run();
      return null;
    };
  }

  private final NoActiveDelegatesAlert noActiveDelegatesAlert =
      NoActiveDelegatesAlert.builder().accountId(ACCOUNT_ID).build();

  @InjectMocks
  private final NoEligibleDelegatesAlert noEligibleDelegatesAlert = aNoEligibleDelegatesAlert()
                                                                        .withAppId(GLOBAL_APP_ID)
                                                                        .withTaskGroup(TaskGroup.JENKINS)
                                                                        .withTaskType(TaskType.JENKINS_COLLECTION)
                                                                        .build();

  private final ApprovalNeededAlert approvalNeededAlert =
      ApprovalNeededAlert.builder().approvalId("approvalId").executionId("executionId").name("name").build();
  private final ManualInterventionNeededAlert manualInterventionNeededAlert =
      ManualInterventionNeededAlert.builder()
          .stateExecutionInstanceId("stateExecutionId")
          .executionId("executionId")
          .name("name")
          .build();

  @Before
  public void setUp() {
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    when(executorService.submit(runnableCaptor.capture())).then(executeRunnable(runnableCaptor));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldListAlerts() {
    alertService.openAlert(ACCOUNT_ID, APP_ID, ApprovalNeeded, approvalNeededAlert);
    PageResponse<Alert> alerts = alertService.list(new PageRequest<>());
    assertThat(alerts).hasSize(1);
    Alert alert = alerts.get(0);
    assertThat(alert.getType()).isEqualTo(ApprovalNeeded);
    assertThat(alert.getAlertData().matches(approvalNeededAlert)).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldOpenAlert() {
    alertService.openAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoActiveDelegates, noActiveDelegatesAlert);

    List<Alert> alerts = alertService.list(new PageRequest<>());
    assertThat(alerts).hasSize(1);
    Alert alert = alerts.get(0);
    assertThat(alert.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(alert.getAppId()).isEqualTo(GLOBAL_APP_ID);
    assertThat(alert.getType()).isEqualTo(NoActiveDelegates);
    assertThat(alert.getCategory()).isEqualTo(NoActiveDelegates.getCategory());
    assertThat(alert.getSeverity()).isEqualTo(NoActiveDelegates.getSeverity());
    assertThat(alert.getTitle()).isEqualTo("No delegates are available");

    verify(eventPublisher).publishEvent(Mockito.any(Event.class));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotOpenMatchingAlert() {
    alertService.openAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoEligibleDelegates, noEligibleDelegatesAlert);
    alertService.openAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoEligibleDelegates, noEligibleDelegatesAlert);
    PageResponse<Alert> alerts = alertService.list(new PageRequest<>());
    assertThat(alerts).hasSize(1);
    Alert alert = alerts.get(0);
    assertThat(alert.getType()).isEqualTo(NoEligibleDelegates);
    assertThat(alert.getAlertData().matches(noEligibleDelegatesAlert)).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldCloseAlert() {
    alertService.openAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoEligibleDelegates, noEligibleDelegatesAlert);
    alertService.closeAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoEligibleDelegates, noEligibleDelegatesAlert);
    PageResponse<Alert> alerts = alertService.list(new PageRequest<>());
    assertThat(alerts).hasSize(1);
    Alert alert = alerts.get(0);
    assertThat(alert.getStatus()).isEqualTo(Closed);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotCloseAlertNoneFound() {
    alertService.closeAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoEligibleDelegates, noEligibleDelegatesAlert);
    assertThat(alertService.list(new PageRequest<>())).hasSize(0);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldCloseAlertsWhenDelegateUpdated() {
    alertService.openAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoActiveDelegates, noActiveDelegatesAlert);
    alertService.openAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoEligibleDelegates, noEligibleDelegatesAlert);
    when(assignDelegateService.canAssign(eq(DELEGATE_ID), any(), any(), any(), any(), any(), any())).thenReturn(true);

    alertService.activeDelegateUpdated(ACCOUNT_ID, DELEGATE_ID);

    PageResponse<Alert> alerts = alertService.list(new PageRequest<>());
    assertThat(alerts).hasSize(2);
    for (Alert alert : alerts) {
      assertThat(alert.getStatus()).isEqualTo(Closed);
    }
  }

  @Test
  @Category(UnitTests.class)
  public void shouldCloseAlertsWhenDeploymentAborted() {
    alertService.openAlert(ACCOUNT_ID, APP_ID, ApprovalNeeded, approvalNeededAlert);
    alertService.openAlert(ACCOUNT_ID, APP_ID, ManualInterventionNeeded, manualInterventionNeededAlert);
    alertService.deploymentCompleted(APP_ID, "executionId");

    PageResponse<Alert> alerts = alertService.list(new PageRequest<>());
    assertThat(alerts).hasSize(2);
    for (Alert alert : alerts) {
      assertThat(alert.getStatus()).isEqualTo(Closed);
    }
  }

  @Test
  @Category(UnitTests.class)
  public void shouldBuildAlertTitle() {
    AlertData alertData = aNoEligibleDelegatesAlert()
                              .withAppId(GLOBAL_APP_ID)
                              .withTaskGroup(TaskGroup.CONTAINER)
                              .withTaskType(TaskType.LIST_CLUSTERS)
                              .build();

    alertService.openAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoEligibleDelegates, alertData);

    List<Alert> alerts = alertService.list(new PageRequest<>());
    assertThat(alerts).hasSize(1);
    Alert alert = alerts.get(0);
    assertThat(alert.getTitle()).isEqualTo("No delegates can execute Container (LIST_CLUSTERS) tasks ");
  }
}
