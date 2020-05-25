package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.GARVIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.alerts.AlertStatus.Closed;
import static software.wings.alerts.AlertStatus.Open;
import static software.wings.alerts.AlertStatus.Pending;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.alert.AlertType.ARTIFACT_COLLECTION_FAILED;
import static software.wings.beans.alert.AlertType.ApprovalNeeded;
import static software.wings.beans.alert.AlertType.ManualInterventionNeeded;
import static software.wings.beans.alert.AlertType.NoActiveDelegates;
import static software.wings.beans.alert.AlertType.NoEligibleDelegates;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;

import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.category.element.UnitTests;
import io.harness.event.model.Event;
import io.harness.event.publisher.EventPublisher;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import software.wings.WingsBaseTest;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskGroup;
import software.wings.beans.TaskType;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.Alert.AlertKeys;
import software.wings.beans.alert.AlertData;
import software.wings.beans.alert.ApprovalNeededAlert;
import software.wings.beans.alert.ArtifactCollectionFailedAlert;
import software.wings.beans.alert.ManualInterventionNeededAlert;
import software.wings.beans.alert.NoActiveDelegatesAlert;
import software.wings.beans.alert.NoEligibleDelegatesAlert;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.SettingsService;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class AlertServiceTest extends WingsBaseTest {
  @Mock private ExecutorService executorService;
  @Mock private AssignDelegateService assignDelegateService;
  @Mock private AppService appService;
  @Mock private SettingsService settingsService;
  @Mock private ArtifactStreamService artifactStreamService;

  @Inject @InjectMocks private AlertService alertService;

  @Mock private EventPublisher eventPublisher;

  private static Answer executeRunnable(ArgumentCaptor<Runnable> runnableCaptor) {
    return invocation -> {
      runnableCaptor.getValue().run();
      return null;
    };
  }

  private final NoActiveDelegatesAlert noActiveDelegatesAlert =
      NoActiveDelegatesAlert.builder().accountId(ACCOUNT_ID).build();

  @InjectMocks
  private final NoEligibleDelegatesAlert noEligibleDelegatesAlert = NoEligibleDelegatesAlert.builder()
                                                                        .appId(GLOBAL_APP_ID)
                                                                        .taskGroup(TaskGroup.JENKINS)
                                                                        .taskType(TaskType.JENKINS_COLLECTION)
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
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldListAlerts() {
    alertService.openAlert(ACCOUNT_ID, APP_ID, ApprovalNeeded, approvalNeededAlert);
    PageResponse<Alert> alerts =
        alertService.list(aPageRequest().addFilter(AlertKeys.accountId, Operator.EQ, ACCOUNT_ID).build());
    assertThat(alerts).hasSize(1);
    Alert alert = alerts.get(0);
    assertThat(alert.getType()).isEqualTo(ApprovalNeeded);
    assertThat(alert.getAlertData().matches(approvalNeededAlert)).isTrue();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldOpenAlert() {
    alertService.openAlert(ACCOUNT_ID, APP_ID, ApprovalNeeded, approvalNeededAlert);

    List<Alert> alerts =
        alertService.list(aPageRequest().addFilter(AlertKeys.accountId, Operator.EQ, ACCOUNT_ID).build());
    assertThat(alerts).hasSize(1);
    Alert alert = alerts.get(0);
    assertThat(alert.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(alert.getAppId()).isEqualTo(APP_ID);
    assertThat(alert.getType()).isEqualTo(ApprovalNeeded);
    assertThat(alert.getCategory()).isEqualTo(ApprovalNeeded.getCategory());
    assertThat(alert.getSeverity()).isEqualTo(ApprovalNeeded.getSeverity());
    assertThat(alert.getTitle()).isEqualTo("name needs approval");
    assertThat(alert.getTriggerCount()).isEqualTo(1);
    assertThat(alert.getStatus()).isEqualTo(Open);

    verify(eventPublisher, times(0)).publishEvent(Mockito.any(Event.class));
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldOpenPendingAlert() {
    alertService.openAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoActiveDelegates, noActiveDelegatesAlert);

    List<Alert> alerts =
        alertService.list(aPageRequest().addFilter(AlertKeys.accountId, Operator.EQ, ACCOUNT_ID).build());
    assertThat(alerts).hasSize(1);
    Alert alert = alerts.get(0);
    assertThat(alert.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(alert.getAppId()).isEqualTo(GLOBAL_APP_ID);
    assertThat(alert.getType()).isEqualTo(NoActiveDelegates);
    assertThat(alert.getCategory()).isEqualTo(NoActiveDelegates.getCategory());
    assertThat(alert.getSeverity()).isEqualTo(NoActiveDelegates.getSeverity());
    assertThat(alert.getTitle()).isEqualTo("No delegates are available");
    assertThat(alert.getTriggerCount()).isEqualTo(1);
    assertThat(alert.getStatus()).isEqualTo(Pending);

    verify(eventPublisher, times(0)).publishEvent(Mockito.any(Event.class));

    alertService.openAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoActiveDelegates, noActiveDelegatesAlert);

    alerts = alertService.list(aPageRequest().addFilter(AlertKeys.accountId, Operator.EQ, ACCOUNT_ID).build());
    assertThat(alerts).hasSize(1);
    alert = alerts.get(0);
    assertThat(alert.getTriggerCount()).isEqualTo(2);
    assertThat(alert.getStatus()).isEqualTo(Pending);

    alertService.openAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoActiveDelegates, noActiveDelegatesAlert);

    alerts = alertService.list(aPageRequest().addFilter(AlertKeys.accountId, Operator.EQ, ACCOUNT_ID).build());
    assertThat(alerts).hasSize(1);
    alert = alerts.get(0);
    assertThat(alert.getTriggerCount()).isEqualTo(3);
    assertThat(alert.getStatus()).isEqualTo(Open);

    verify(eventPublisher, times(1)).publishEvent(Mockito.any(Event.class));
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotOpenMatchingAlert() {
    alertService.openAlert(ACCOUNT_ID, APP_ID, ApprovalNeeded, approvalNeededAlert);
    alertService.openAlert(ACCOUNT_ID, APP_ID, ApprovalNeeded, approvalNeededAlert);
    PageResponse<Alert> alerts =
        alertService.list(aPageRequest().addFilter(AlertKeys.accountId, Operator.EQ, ACCOUNT_ID).build());
    assertThat(alerts).hasSize(1);
    Alert alert = alerts.get(0);
    assertThat(alert.getType()).isEqualTo(ApprovalNeeded);
    assertThat(alert.getAlertData().matches(approvalNeededAlert)).isTrue();
    assertThat(alert.getTriggerCount()).isEqualTo(2);
    assertThat(alert.getStatus()).isEqualTo(Open);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldCloseAlert() {
    alertService.openAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoEligibleDelegates, noEligibleDelegatesAlert);
    alertService.closeAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoEligibleDelegates, noEligibleDelegatesAlert);
    PageResponse<Alert> alerts =
        alertService.list(aPageRequest().addFilter(AlertKeys.accountId, Operator.EQ, ACCOUNT_ID).build());
    assertThat(alerts).hasSize(1);
    Alert alert = alerts.get(0);
    assertThat(alert.getStatus()).isEqualTo(Closed);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotCloseAlertNoneFound() {
    alertService.closeAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoEligibleDelegates, noEligibleDelegatesAlert);
    assertThat(alertService.list(aPageRequest().addFilter(AlertKeys.accountId, Operator.EQ, ACCOUNT_ID).build()))
        .hasSize(0);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  @Ignore("The test is ignored until the reconcillation performance is fixed.")
  public void shouldCloseAlertsWhenDelegateUpdated() {
    alertService.openAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoActiveDelegates, noActiveDelegatesAlert);
    alertService.openAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoEligibleDelegates, noEligibleDelegatesAlert);
    when(assignDelegateService.canAssign(eq(null), eq(DELEGATE_ID), any(), any(), any(), any(), any(), any()))
        .thenReturn(true);

    alertService.activeDelegateUpdated(ACCOUNT_ID, DELEGATE_ID);

    PageResponse<Alert> alerts =
        alertService.list(aPageRequest().addFilter(AlertKeys.accountId, Operator.EQ, ACCOUNT_ID).build());
    assertThat(alerts).hasSize(2);
    for (Alert alert : alerts) {
      assertThat(alert.getStatus()).isEqualTo(Closed);
    }
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldCloseAlertsWhenDeploymentAborted() {
    alertService.openAlert(ACCOUNT_ID, APP_ID, ApprovalNeeded, approvalNeededAlert);
    alertService.openAlert(ACCOUNT_ID, APP_ID, ManualInterventionNeeded, manualInterventionNeededAlert);
    alertService.deploymentCompleted(APP_ID, "executionId");

    PageResponse<Alert> alerts =
        alertService.list(aPageRequest().addFilter(AlertKeys.accountId, Operator.EQ, ACCOUNT_ID).build());
    assertThat(alerts).hasSize(2);
    for (Alert alert : alerts) {
      assertThat(alert.getStatus()).isEqualTo(Closed);
    }
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldBuildAlertTitle() {
    AlertData alertData = NoEligibleDelegatesAlert.builder()
                              .appId(GLOBAL_APP_ID)
                              .taskGroup(TaskGroup.CONTAINER)
                              .taskType(TaskType.LIST_CLUSTERS)
                              .build();

    alertService.openAlert(ACCOUNT_ID, GLOBAL_APP_ID, NoEligibleDelegates, alertData);

    List<Alert> alerts =
        alertService.list(aPageRequest().addFilter(AlertKeys.accountId, Operator.EQ, ACCOUNT_ID).build());
    assertThat(alerts).hasSize(1);
    Alert alert = alerts.get(0);
    assertThat(alert.getTitle()).isEqualTo("No delegates can execute Container (LIST_CLUSTERS) tasks ");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldDeleteByArtifactStream() {
    AlertData alertData = ArtifactCollectionFailedAlert.builder()
                              .appId(APP_ID)
                              .serviceId(SERVICE_ID)
                              .artifactStreamId(ARTIFACT_STREAM_ID)
                              .build();

    alertService.openAlert(ACCOUNT_ID, null, ARTIFACT_COLLECTION_FAILED, alertData);
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);

    List<Alert> alerts =
        alertService.list(aPageRequest().addFilter(AlertKeys.accountId, Operator.EQ, ACCOUNT_ID).build());
    assertThat(alerts).hasSize(1);

    alertService.deleteByArtifactStream(APP_ID, ARTIFACT_STREAM_ID);

    alerts = alertService.list(aPageRequest().addFilter(AlertKeys.accountId, Operator.EQ, ACCOUNT_ID).build());
    assertThat(alerts).hasSize(0);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldDeleteByArtifactStreamAtConnectorLevel() {
    AlertData alertData = ArtifactCollectionFailedAlert.builder().artifactStreamId(ARTIFACT_STREAM_ID).build();

    alertService.openAlert(ACCOUNT_ID, null, ARTIFACT_COLLECTION_FAILED, alertData);
    when(artifactStreamService.get(ARTIFACT_STREAM_ID))
        .thenReturn(DockerArtifactStream.builder().settingId(SETTING_ID).build());
    when(settingsService.get(SETTING_ID))
        .thenReturn(SettingAttribute.Builder.aSettingAttribute().withAccountId(ACCOUNT_ID).build());

    List<Alert> alerts =
        alertService.list(aPageRequest().addFilter(AlertKeys.accountId, Operator.EQ, ACCOUNT_ID).build());
    assertThat(alerts).hasSize(1);

    alertService.deleteByArtifactStream(GLOBAL_APP_ID, ARTIFACT_STREAM_ID);

    alerts = alertService.list(aPageRequest().addFilter(AlertKeys.accountId, Operator.EQ, ACCOUNT_ID).build());
    assertThat(alerts).hasSize(0);
  }
}
