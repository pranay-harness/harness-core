package io.harness.cvng.statemachine.entities;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.analysis.entities.LearningEngineTask.ExecutionStatus;
import io.harness.cvng.analysis.services.api.LogClusterService;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.rule.Owner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ServiceGuardLogClusterStateTest extends CategoryTest {
  private String verificationTaskId;
  private Instant startTime;
  private Instant endTime;
  @Mock private LogClusterService logClusterService;

  private ServiceGuardLogClusterState serviceGuardLogClusterState;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);

    verificationTaskId = generateUuid();
    startTime = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    endTime = startTime.plus(5, ChronoUnit.MINUTES);

    AnalysisInput input =
        AnalysisInput.builder().verificationTaskId(verificationTaskId).startTime(startTime).endTime(endTime).build();

    serviceGuardLogClusterState = ServiceGuardLogClusterState.builder().clusterLevel(LogClusterLevel.L1).build();
    serviceGuardLogClusterState.setInputs(input);
    serviceGuardLogClusterState.setStatus(AnalysisStatus.CREATED);
    serviceGuardLogClusterState.setLogClusterService(logClusterService);

    when(logClusterService.scheduleL1ClusteringTasks(any())).thenReturn(Arrays.asList(generateUuid()));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testExecute() {
    serviceGuardLogClusterState.execute();

    assertThat(serviceGuardLogClusterState.getStatus().name()).isEqualTo(AnalysisStatus.RUNNING.name());
    assertThat(serviceGuardLogClusterState.getWorkerTaskIds()).isNotNull();
    assertThat(serviceGuardLogClusterState.getRetryCount()).isEqualTo(0);
  }

  @Test(expected = IllegalStateException.class)
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testExecute_noTasksCreated() {
    when(logClusterService.scheduleL1ClusteringTasks(any())).thenReturn(null);
    serviceGuardLogClusterState.execute();

    assertThat(serviceGuardLogClusterState.getStatus().name()).isEqualTo(AnalysisStatus.RUNNING.name());
    assertThat(serviceGuardLogClusterState.getWorkerTaskIds()).isNotNull();
    assertThat(serviceGuardLogClusterState.getRetryCount()).isEqualTo(0);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetExecutionStatus() {
    Map<String, ExecutionStatus> taskStatusMap = new HashMap<>();
    taskStatusMap.put(generateUuid(), ExecutionStatus.SUCCESS);
    taskStatusMap.put(generateUuid(), ExecutionStatus.SUCCESS);
    taskStatusMap.put(generateUuid(), ExecutionStatus.SUCCESS);

    serviceGuardLogClusterState.setWorkerTaskIds(taskStatusMap.keySet());
    when(logClusterService.getTaskStatus(any())).thenReturn(taskStatusMap);
    AnalysisStatus status = serviceGuardLogClusterState.getExecutionStatus();

    assertThat(status.name()).isEqualTo(AnalysisStatus.TRANSITION.name());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetExecutionStatus_someRunning() {
    Map<String, ExecutionStatus> taskStatusMap = new HashMap<>();
    taskStatusMap.put(generateUuid(), ExecutionStatus.SUCCESS);
    taskStatusMap.put(generateUuid(), ExecutionStatus.RUNNING);
    taskStatusMap.put(generateUuid(), ExecutionStatus.SUCCESS);
    serviceGuardLogClusterState.setWorkerTaskIds(taskStatusMap.keySet());
    when(logClusterService.getTaskStatus(any())).thenReturn(taskStatusMap);
    AnalysisStatus status = serviceGuardLogClusterState.getExecutionStatus();

    assertThat(status.name()).isEqualTo(AnalysisStatus.RUNNING.name());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetExecutionStatus_someQueued() {
    Map<String, ExecutionStatus> taskStatusMap = new HashMap<>();
    taskStatusMap.put(generateUuid(), ExecutionStatus.SUCCESS);
    taskStatusMap.put(generateUuid(), ExecutionStatus.QUEUED);
    taskStatusMap.put(generateUuid(), ExecutionStatus.SUCCESS);
    serviceGuardLogClusterState.setWorkerTaskIds(taskStatusMap.keySet());
    when(logClusterService.getTaskStatus(any())).thenReturn(taskStatusMap);
    AnalysisStatus status = serviceGuardLogClusterState.getExecutionStatus();

    assertThat(status.name()).isEqualTo(AnalysisStatus.RUNNING.name());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testHandleTransition_L1() {
    AnalysisState state = serviceGuardLogClusterState.handleTransition();
    assertThat(state).isNotNull();
    assertThat(state instanceof ServiceGuardLogClusterState).isTrue();
    ServiceGuardLogClusterState newState = (ServiceGuardLogClusterState) state;
    assertThat(newState.getInputs()).isEqualTo(serviceGuardLogClusterState.getInputs());
    assertThat(newState.getClusterLevel().name()).isEqualTo(LogClusterLevel.L2.name());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testHandleTransition_L2() {
    serviceGuardLogClusterState.setClusterLevel(LogClusterLevel.L2);
    AnalysisState state = serviceGuardLogClusterState.handleTransition();
    assertThat(state).isNotNull();
    assertThat(state instanceof ServiceGuardLogAnalysisState).isTrue();
    ServiceGuardLogAnalysisState newState = (ServiceGuardLogAnalysisState) state;
    assertThat(newState.getInputs()).isEqualTo(serviceGuardLogClusterState.getInputs());
  }
}
