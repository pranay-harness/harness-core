package io.harness.event.reconciliation.service;

import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.ExecutionStatus.WAITING;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RUSHABH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.CountsByStatuses.Builder.aCountsByStatuses;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.event.reconciliation.deployment.DeploymentReconRecord;
import io.harness.event.reconciliation.deployment.DetectionStatus;
import io.harness.event.reconciliation.deployment.ReconcilationAction;
import io.harness.event.reconciliation.deployment.ReconciliationStatus;
import io.harness.rule.Owner;
import io.harness.timescaledb.TimeScaleDBService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder;
import software.wings.dl.WingsPersistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class DeploymentReconServiceImplTest extends WingsBaseTest {
  @Mock TimeScaleDBService timeScaleDBService;
  @Inject @InjectMocks DeploymentReconServiceImpl deploymentReconService;
  final Connection mockConnection = mock(Connection.class);
  final Statement mockStatement = mock(Statement.class);
  final ResultSet preparedStatementResultSet = mock(ResultSet.class);
  final ResultSet statementResultSet = mock(ResultSet.class);
  final PreparedStatement preparedStatement = mock(PreparedStatement.class);
  int[] count = {0};

  @Before
  public void setUp() throws Exception {
    deploymentReconService = spy(deploymentReconService);
    when(timeScaleDBService.getDBConnection()).thenReturn(mockConnection);
    when(mockConnection.createStatement()).thenReturn(mockStatement);
    when(mockConnection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(preparedStatementResultSet);
    when(mockStatement.executeQuery(anyString())).thenReturn(statementResultSet);
    when(timeScaleDBService.isValid()).thenReturn(true);
  }

  public void activateDuplicatesFound() throws Exception {
    when(preparedStatementResultSet.next()).thenAnswer(new Answer<Boolean>() {
      @Override
      public Boolean answer(InvocationOnMock invocation) throws Throwable {
        while (count[0] < 2) {
          count[0]++;
          return true;
        }
        return false;
      }
    });
  }

  private void activateMissingRecords(long timeStamp) {
    CountsByStatuses countsByStatuses = aCountsByStatuses().build();

    final WorkflowExecutionBuilder workflowExecutionBuilder = WorkflowExecution.builder()
                                                                  .accountId(ACCOUNT_ID)
                                                                  .appId(APP_ID)
                                                                  .envId(ENV_ID)
                                                                  .startTs(2L)
                                                                  .endTs(timeStamp)
                                                                  .createdAt(timeStamp);

    wingsPersistence.save(workflowExecutionBuilder.uuid(generateUuid()).status(SUCCESS).build());
    wingsPersistence.save(workflowExecutionBuilder.uuid(generateUuid())
                              .status(ExecutionStatus.ERROR)
                              .breakdown(countsByStatuses)
                              .build());
    wingsPersistence.save(workflowExecutionBuilder.uuid(generateUuid())
                              .status(ExecutionStatus.FAILED)
                              .breakdown(countsByStatuses)
                              .build());
    wingsPersistence.save(workflowExecutionBuilder.uuid(generateUuid()).status(ExecutionStatus.ABORTED).build());

    wingsPersistence.save(workflowExecutionBuilder.uuid(generateUuid()).status(RUNNING).build());

    wingsPersistence.save(workflowExecutionBuilder.uuid(generateUuid()).status(PAUSED).build());
    wingsPersistence.save(workflowExecutionBuilder.uuid(generateUuid()).status(WAITING).build());
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testAllOK() {
    try {
      final long durationStartTs = System.currentTimeMillis() - 2000000;
      final long durationEndTs = System.currentTimeMillis();
      deploymentReconService.performReconciliation(ACCOUNT_ID, durationStartTs, durationEndTs);
      DeploymentReconRecord latestRecord = deploymentReconService.getLatestDeploymentReconRecord(ACCOUNT_ID);
      assertThat(latestRecord.getAccountId()).isEqualTo(ACCOUNT_ID);
      assertThat(latestRecord.getDurationStartTs()).isEqualTo(durationStartTs);
      assertThat(latestRecord.getDurationEndTs()).isEqualTo(durationEndTs);
      assertThat(latestRecord.getDetectionStatus()).isEqualTo(DetectionStatus.SUCCESS);
      assertThat(latestRecord.getReconcilationAction()).isEqualTo(ReconcilationAction.NONE);
      assertThat(latestRecord.getReconciliationStatus()).isEqualTo(ReconciliationStatus.SUCCESS);

      ReconciliationStatus status =
          deploymentReconService.performReconciliation(ACCOUNT_ID, durationStartTs, durationEndTs);
      assertThat(status).isEqualTo(ReconciliationStatus.SUCCESS);

    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testMissingRecords() throws Exception {
    final long durationStartTs = System.currentTimeMillis() - 2000000;
    final long durationEndTs = System.currentTimeMillis();
    activateMissingRecords(durationEndTs - 1000L);
    deploymentReconService.performReconciliation(ACCOUNT_ID, durationStartTs, durationEndTs);
    DeploymentReconRecord latestRecord = deploymentReconService.getLatestDeploymentReconRecord(ACCOUNT_ID);
    assertThat(latestRecord.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(latestRecord.getDurationStartTs()).isEqualTo(durationStartTs);
    assertThat(latestRecord.getDurationEndTs()).isEqualTo(durationEndTs);
    assertThat(latestRecord.getDetectionStatus()).isEqualTo(DetectionStatus.MISSING_RECORDS_DETECTED);
    assertThat(latestRecord.getReconcilationAction()).isEqualTo(ReconcilationAction.ADD_MISSING_RECORDS);
    assertThat(latestRecord.getReconciliationStatus()).isEqualTo(ReconciliationStatus.SUCCESS);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testMissingRecordsDuplicatesFound() throws Exception {
    activateDuplicatesFound();
    final long durationStartTs = System.currentTimeMillis() - 2000000;
    final long durationEndTs = System.currentTimeMillis();
    activateMissingRecords(durationEndTs - 1300);
    deploymentReconService.performReconciliation(ACCOUNT_ID, durationStartTs, durationEndTs);
    DeploymentReconRecord latestRecord = deploymentReconService.getLatestDeploymentReconRecord(ACCOUNT_ID);
    assertThat(latestRecord.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(latestRecord.getDurationStartTs()).isEqualTo(durationStartTs);
    assertThat(latestRecord.getDurationEndTs()).isEqualTo(durationEndTs);
    assertThat(latestRecord.getDetectionStatus())
        .isEqualTo(DetectionStatus.DUPLICATE_DETECTED_MISSING_RECORDS_DETECTED);
    assertThat(latestRecord.getReconcilationAction())
        .isEqualTo(ReconcilationAction.DUPLICATE_REMOVAL_ADD_MISSING_RECORDS);
    assertThat(latestRecord.getReconciliationStatus()).isEqualTo(ReconciliationStatus.SUCCESS);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testDuplicatesDetected() {
    try {
      activateDuplicatesFound();
      when(preparedStatementResultSet.getString(anyInt())).thenReturn("DATA" + count[0]);

      final long durationStartTs = System.currentTimeMillis() - 2000000;
      final long durationEndTs = System.currentTimeMillis();
      deploymentReconService.performReconciliation(ACCOUNT_ID, durationStartTs, durationEndTs);
      DeploymentReconRecord latestRecord = deploymentReconService.getLatestDeploymentReconRecord(ACCOUNT_ID);
      assertThat(latestRecord.getAccountId()).isEqualTo(ACCOUNT_ID);
      assertThat(latestRecord.getDurationStartTs()).isEqualTo(durationStartTs);
      assertThat(latestRecord.getDurationEndTs()).isEqualTo(durationEndTs);
      assertThat(latestRecord.getDetectionStatus()).isEqualTo(DetectionStatus.DUPLICATE_DETECTED);
      assertThat(latestRecord.getReconcilationAction()).isEqualTo(ReconcilationAction.DUPLICATE_REMOVAL);
      assertThat(latestRecord.getReconciliationStatus()).isEqualTo(ReconciliationStatus.SUCCESS);

    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testShouldPerformReconciliation() {
    DeploymentReconRecord reconRecord = DeploymentReconRecord.builder()
                                            .reconciliationStatus(ReconciliationStatus.IN_PROGRESS)
                                            .durationEndTs(System.currentTimeMillis() - 1000)
                                            .build();

    wingsPersistence = mock(WingsPersistence.class);
    on(deploymentReconService).set("wingsPersistence", wingsPersistence);

    assertThat(deploymentReconService.shouldPerformReconciliation(reconRecord, System.currentTimeMillis() - 1000))
        .isFalse();

    reconRecord = DeploymentReconRecord.builder()
                      .reconciliationStatus(ReconciliationStatus.IN_PROGRESS)
                      .durationEndTs(System.currentTimeMillis() - 2 * DeploymentReconServiceImpl.COOL_DOWN_INTERVAL)
                      .build();

    final UpdateOperations updateOperations = mock(UpdateOperations.class);
    when(wingsPersistence.createUpdateOperations(DeploymentReconRecord.class)).thenReturn(updateOperations);
    assertThat(deploymentReconService.shouldPerformReconciliation(
                   reconRecord, System.currentTimeMillis() - 2 * DeploymentReconServiceImpl.COOL_DOWN_INTERVAL))
        .isTrue();

    verify(wingsPersistence, times(1)).createUpdateOperations(DeploymentReconRecord.class);
    verify(wingsPersistence, times(1)).update(reconRecord, updateOperations);

    reconRecord = DeploymentReconRecord.builder()
                      .reconciliationStatus(ReconciliationStatus.SUCCESS)
                      .durationEndTs(System.currentTimeMillis() - 2 * DeploymentReconServiceImpl.COOL_DOWN_INTERVAL)
                      .build();

    assertThat(deploymentReconService.shouldPerformReconciliation(reconRecord, System.currentTimeMillis())).isTrue();

    reconRecord = DeploymentReconRecord.builder()
                      .reconciliationStatus(ReconciliationStatus.SUCCESS)
                      .reconEndTs(System.currentTimeMillis() - 1000)
                      .durationEndTs(System.currentTimeMillis() - 1000)
                      .build();
    assertThat(deploymentReconService.shouldPerformReconciliation(reconRecord, System.currentTimeMillis() - 1000))
        .isFalse();
  }
}
