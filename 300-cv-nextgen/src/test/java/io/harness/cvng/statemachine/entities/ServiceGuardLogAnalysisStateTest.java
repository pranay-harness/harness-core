package io.harness.cvng.statemachine.entities;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.analysis.entities.LearningEngineTask.ExecutionStatus;
import io.harness.cvng.analysis.services.api.LogAnalysisService;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.rule.Owner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ServiceGuardLogAnalysisStateTest extends CategoryTest {
  private String verificationTaskId;
  private Instant startTime;
  private Instant endTime;
  @Mock private LogAnalysisService logAnalysisService;

  private ServiceGuardLogAnalysisState logAnalysisState;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);

    verificationTaskId = generateUuid();
    startTime = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    endTime = startTime.plus(5, ChronoUnit.MINUTES);

    AnalysisInput input =
        AnalysisInput.builder().verificationTaskId(verificationTaskId).startTime(startTime).endTime(endTime).build();

    logAnalysisState = ServiceGuardLogAnalysisState.builder().build();
    logAnalysisState.setInputs(input);
    logAnalysisState.setLogAnalysisService(logAnalysisService);

    when(logAnalysisService.scheduleServiceGuardLogAnalysisTask(any())).thenReturn(generateUuid());
  }
  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testExecute() {
    logAnalysisState.execute();

    assertThat(logAnalysisState.getStatus().name()).isEqualTo(AnalysisStatus.RUNNING.name());
    assertThat(logAnalysisState.getWorkerTaskId()).isNotNull();
    assertThat(logAnalysisState.getRetryCount()).isEqualTo(0);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetExecutionStatus_success() {
    String taskId = generateUuid();
    logAnalysisState.setStatus(AnalysisStatus.RUNNING);
    logAnalysisState.setWorkerTaskId(taskId);
    Map<String, ExecutionStatus> taskStatusMap = new HashMap<>();
    taskStatusMap.put(taskId, ExecutionStatus.SUCCESS);

    when(logAnalysisService.getTaskStatus(anyList())).thenReturn(taskStatusMap);

    AnalysisStatus status = logAnalysisState.getExecutionStatus();

    assertThat(status.name()).isEqualTo(AnalysisStatus.TRANSITION.name());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetExecutionStatus_running() {
    String taskId = generateUuid();
    logAnalysisState.setStatus(AnalysisStatus.RUNNING);
    logAnalysisState.setWorkerTaskId(taskId);
    Map<String, ExecutionStatus> taskStatusMap = new HashMap<>();
    taskStatusMap.put(taskId, ExecutionStatus.RUNNING);

    when(logAnalysisService.getTaskStatus(anyList())).thenReturn(taskStatusMap);

    AnalysisStatus status = logAnalysisState.getExecutionStatus();

    assertThat(status.name()).isEqualTo(AnalysisStatus.RUNNING.name());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetExecutionStatus_failed() {
    String taskId = generateUuid();
    logAnalysisState.setStatus(AnalysisStatus.RUNNING);
    logAnalysisState.setWorkerTaskId(taskId);
    Map<String, ExecutionStatus> taskStatusMap = new HashMap<>();
    taskStatusMap.put(taskId, ExecutionStatus.FAILED);

    when(logAnalysisService.getTaskStatus(anyList())).thenReturn(taskStatusMap);

    AnalysisStatus status = logAnalysisState.getExecutionStatus();

    assertThat(status.name()).isEqualTo(AnalysisStatus.RETRY.name());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetExecutionStatus_timeout() {
    String taskId = generateUuid();
    logAnalysisState.setStatus(AnalysisStatus.RUNNING);
    logAnalysisState.setWorkerTaskId(taskId);
    Map<String, ExecutionStatus> taskStatusMap = new HashMap<>();
    taskStatusMap.put(taskId, ExecutionStatus.TIMEOUT);

    when(logAnalysisService.getTaskStatus(anyList())).thenReturn(taskStatusMap);

    AnalysisStatus status = logAnalysisState.getExecutionStatus();

    assertThat(status.name()).isEqualTo(AnalysisStatus.RETRY.name());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetExecutionStatus_queued() {
    String taskId = generateUuid();
    logAnalysisState.setStatus(AnalysisStatus.RUNNING);
    logAnalysisState.setWorkerTaskId(taskId);
    Map<String, ExecutionStatus> taskStatusMap = new HashMap<>();
    taskStatusMap.put(taskId, ExecutionStatus.QUEUED);

    when(logAnalysisService.getTaskStatus(anyList())).thenReturn(taskStatusMap);

    AnalysisStatus status = logAnalysisState.getExecutionStatus();

    assertThat(status.name()).isEqualTo(AnalysisStatus.RUNNING.name());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testHandleRerun() {
    logAnalysisState.setRetryCount(2);
    logAnalysisState.setStatus(AnalysisStatus.FAILED);

    logAnalysisState.handleRerun();

    assertThat(logAnalysisState.getStatus().name()).isEqualTo(AnalysisStatus.RUNNING.name());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testHandleRunning() {
    logAnalysisState.setStatus(AnalysisStatus.RUNNING);

    logAnalysisState.handleRunning();

    assertThat(logAnalysisState.getStatus().name()).isEqualTo(AnalysisStatus.RUNNING.name());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testHandleSuccess() {
    AnalysisState state = logAnalysisState.handleSuccess();
    assertThat(state.getStatus().name()).isEqualTo(AnalysisStatus.SUCCESS.name());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testHandleTransition() {
    AnalysisState state = logAnalysisState.handleTransition();
    assertThat(state.getStatus().name()).isEqualTo(AnalysisStatus.CREATED.name());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testHandleRetry() {
    logAnalysisState.setRetryCount(1);

    logAnalysisState.handleRetry();

    assertThat(logAnalysisState.getStatus().name()).isEqualTo(AnalysisStatus.RUNNING.name());
    assertThat(logAnalysisState.getWorkerTaskId()).isNotNull();
    assertThat(logAnalysisState.getRetryCount()).isEqualTo(2);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testHandleRetry_noMoreRetry() {
    logAnalysisState.setRetryCount(2);

    logAnalysisState.handleRetry();

    assertThat(logAnalysisState.getStatus().name()).isEqualTo(AnalysisStatus.FAILED.name());
  }
}
