package software.wings.service;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.alert.Alert.AlertBuilder.anAlert;
import static software.wings.beans.alert.AlertType.ApprovalNeeded;
import static software.wings.beans.alert.AlertType.ManualInterventionNeeded;
import static software.wings.beans.alert.AlertType.NoActiveDelegates;
import static software.wings.beans.alert.AlertType.NoEligibleDelegates;
import static software.wings.beans.alert.NoEligibleDelegatesAlert.NoEligibleDelegatesAlertBuilder.aNoEligibleDelegatesAlert;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;

import com.google.inject.Inject;

import com.mongodb.DBCollection;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.persistence.HQuery;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.alerts.AlertStatus;
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

import java.util.concurrent.ExecutorService;

public class AlertServiceTest extends WingsBaseTest {
  @Mock private WingsPersistence wingsPersistence;
  @Mock private ExecutorService executorService;
  @Mock private AssignDelegateService assignDelegateService;

  @Inject @InjectMocks private AlertService alertService;

  @Mock private HQuery<Alert> query;
  @Mock private FieldEnd end;
  @Mock private UpdateOperations updateOperations;
  @Mock private DBCollection alertsCollection;

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

  private final Alert noActive = anAlert()
                                     .withAccountId(ACCOUNT_ID)
                                     .withAppId(GLOBAL_APP_ID)
                                     .withAlertData(noActiveDelegatesAlert)
                                     .withType(NoActiveDelegates)
                                     .withStatus(AlertStatus.Open)
                                     .build();
  private final Alert noEligible = anAlert()
                                       .withAccountId(ACCOUNT_ID)
                                       .withAppId(GLOBAL_APP_ID)
                                       .withAlertData(noEligibleDelegatesAlert)
                                       .withType(NoEligibleDelegates)
                                       .withStatus(AlertStatus.Open)
                                       .build();
  private final Alert approval =
      anAlert()
          .withAccountId(ACCOUNT_ID)
          .withAppId(APP_ID)
          .withType(ApprovalNeeded)
          .withAlertData(
              ApprovalNeededAlert.builder().approvalId("approvalId").executionId("executionId").name("name").build())
          .withStatus(AlertStatus.Open)
          .build();

  private final Alert manualIntervention = anAlert()
                                               .withAccountId(ACCOUNT_ID)
                                               .withAppId(APP_ID)
                                               .withType(ManualInterventionNeeded)
                                               .withAlertData(ManualInterventionNeededAlert.builder()
                                                                  .stateExecutionInstanceId("stateExecutionId")
                                                                  .executionId("executionId")
                                                                  .name("name")
                                                                  .build())
                                               .withStatus(AlertStatus.Open)
                                               .build();

  @Before
  public void setUp() {
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    when(executorService.submit(runnableCaptor.capture())).then(executeRunnable(runnableCaptor));
    when(wingsPersistence.createUpdateOperations(Alert.class)).thenReturn(updateOperations);
    when(wingsPersistence.createQuery(Alert.class)).thenReturn(query);
    when(wingsPersistence.createQuery(Alert.class, excludeAuthority)).thenReturn(query);
    when(query.filter(any(), any())).thenReturn(query);
    when(query.field(any())).thenReturn(end);
    when(end.lessThan(any())).thenReturn(query);
    when(end.in(any())).thenReturn(query);
  }

  @Test
  public void shouldListAlerts() {
    PageRequest<Alert> pageRequest = new PageRequest<>();
    PageResponse pageResponse = aPageResponse().withResponse(singletonList(approval)).build();
    when(wingsPersistence.query(Alert.class, pageRequest)).thenReturn(pageResponse);

    PageResponse<Alert> alerts = alertService.list(pageRequest);

    assertThat(alerts).hasSize(1).containsExactly(approval);
    verify(wingsPersistence).query(Alert.class, pageRequest);
  }

  @Test
  public void shouldOpenAlert() {
    when(query.asList()).thenReturn(emptyList());

    alertService.openAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoActiveDelegates, noActiveDelegatesAlert);

    ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
    verify(wingsPersistence).saveAndGet(eq(Alert.class), alertCaptor.capture());
    Alert savedAlert = alertCaptor.getValue();
    assertThat(savedAlert.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(savedAlert.getAppId()).isEqualTo(GLOBAL_APP_ID);
    assertThat(savedAlert.getType()).isEqualTo(NoActiveDelegates);
    assertThat(savedAlert.getCategory()).isEqualTo(NoActiveDelegates.getCategory());
    assertThat(savedAlert.getSeverity()).isEqualTo(NoActiveDelegates.getSeverity());
    assertThat(savedAlert.getTitle()).isEqualTo("No delegates are available");
  }

  @Test
  public void shouldNotOpenMatchingAlert() {
    when(query.asList()).thenReturn(singletonList(noEligible));

    alertService.openAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoEligibleDelegates, noEligibleDelegatesAlert);

    verify(wingsPersistence, times(0)).saveAndGet(any(), any());
  }

  @Test
  public void shouldCloseAlert() {
    when(query.asList()).thenReturn(singletonList(noEligible));

    alertService.closeAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoEligibleDelegates, noEligibleDelegatesAlert);

    verify(wingsPersistence).update(any(Query.class), any(UpdateOperations.class));
  }

  @Test
  public void shouldNotCloseAlertNoneFound() {
    when(query.asList()).thenReturn(emptyList());

    alertService.closeAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoEligibleDelegates, noEligibleDelegatesAlert);

    verify(wingsPersistence, times(0)).update(any(Query.class), any(UpdateOperations.class));
  }

  @Test
  public void shouldCloseAlertsWhenDelegateUpdated() {
    when(query.asList()).thenReturn(singletonList(noActive)).thenReturn(singletonList(noEligible));
    when(assignDelegateService.canAssign(eq(DELEGATE_ID), any(), any(), any(), any(), any(), any())).thenReturn(true);

    alertService.activeDelegateUpdated(ACCOUNT_ID, DELEGATE_ID);

    verify(wingsPersistence, times(2)).update(any(Query.class), any(UpdateOperations.class));
  }

  @Test
  public void shouldCloseAlertsWhenDeploymentAborted() {
    when(query.asList()).thenReturn(asList(approval, manualIntervention));
    alertService.deploymentCompleted(APP_ID, "executionId");

    verify(wingsPersistence, times(2)).update(any(Query.class), any(UpdateOperations.class));
  }

  @Test
  public void shouldBuildAlertTitle() {
    AlertData alertData = aNoEligibleDelegatesAlert()
                              .withAppId(GLOBAL_APP_ID)
                              .withTaskGroup(TaskGroup.CONTAINER)
                              .withTaskType(TaskType.LIST_CLUSTERS)
                              .build();
    when(query.asList()).thenReturn(emptyList());

    alertService.openAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoEligibleDelegates, alertData);

    ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
    verify(wingsPersistence).saveAndGet(eq(Alert.class), alertCaptor.capture());
    Alert savedAlert = alertCaptor.getValue();
    assertThat(savedAlert.getTitle()).isEqualTo("No delegates can execute Container (LIST_CLUSTERS) tasks ");
  }
}
