/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.statemachine.entities;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.analysis.entities.LearningEngineTask.ExecutionStatus;
import io.harness.cvng.analysis.services.api.TrendAnalysisService;
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

public class ServiceGuardTrendAnalysisStateTest extends CvNextGenTestBase {
  private String verificationTaskId;
  private Instant startTime;
  private Instant endTime;
  @Mock private TrendAnalysisService trendAnalysisService;

  private ServiceGuardTrendAnalysisState trendAnalysisState;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);

    verificationTaskId = generateUuid();
    startTime = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    endTime = startTime.plus(5, ChronoUnit.MINUTES);

    AnalysisInput input =
        AnalysisInput.builder().verificationTaskId(verificationTaskId).startTime(startTime).endTime(endTime).build();

    trendAnalysisState = ServiceGuardTrendAnalysisState.builder().build();
    trendAnalysisState.setInputs(input);
    trendAnalysisState.setTrendAnalysisService(trendAnalysisService);

    when(trendAnalysisService.scheduleTrendAnalysisTask(any())).thenReturn(generateUuid());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testExecute() {
    trendAnalysisState.execute();

    assertThat(trendAnalysisState.getStatus().name()).isEqualTo(AnalysisStatus.RUNNING.name());
    assertThat(trendAnalysisState.getWorkerTaskId()).isNotNull();
    assertThat(trendAnalysisState.getRetryCount()).isEqualTo(0);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetExecutionStatus_success() {
    String taskId = generateUuid();
    trendAnalysisState.setStatus(AnalysisStatus.RUNNING);
    trendAnalysisState.setWorkerTaskId(taskId);
    Map<String, ExecutionStatus> taskStatusMap = new HashMap<>();
    taskStatusMap.put(taskId, ExecutionStatus.SUCCESS);

    when(trendAnalysisService.getTaskStatus(anyList())).thenReturn(taskStatusMap);

    AnalysisStatus status = trendAnalysisState.getExecutionStatus();

    assertThat(status.name()).isEqualTo(AnalysisStatus.SUCCESS.name());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetExecutionStatus_running() {
    String taskId = generateUuid();
    trendAnalysisState.setStatus(AnalysisStatus.RUNNING);
    trendAnalysisState.setWorkerTaskId(taskId);
    Map<String, ExecutionStatus> taskStatusMap = new HashMap<>();
    taskStatusMap.put(taskId, ExecutionStatus.RUNNING);

    when(trendAnalysisService.getTaskStatus(anyList())).thenReturn(taskStatusMap);

    AnalysisStatus status = trendAnalysisState.getExecutionStatus();

    assertThat(status.name()).isEqualTo(AnalysisStatus.RUNNING.name());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetExecutionStatus_failed() {
    String taskId = generateUuid();
    trendAnalysisState.setStatus(AnalysisStatus.RUNNING);
    trendAnalysisState.setWorkerTaskId(taskId);
    Map<String, ExecutionStatus> taskStatusMap = new HashMap<>();
    taskStatusMap.put(taskId, ExecutionStatus.FAILED);

    when(trendAnalysisService.getTaskStatus(anyList())).thenReturn(taskStatusMap);

    AnalysisStatus status = trendAnalysisState.getExecutionStatus();

    assertThat(status.name()).isEqualTo(AnalysisStatus.RETRY.name());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetExecutionStatus_timeout() {
    String taskId = generateUuid();
    trendAnalysisState.setStatus(AnalysisStatus.RUNNING);
    trendAnalysisState.setWorkerTaskId(taskId);
    Map<String, ExecutionStatus> taskStatusMap = new HashMap<>();
    taskStatusMap.put(taskId, ExecutionStatus.TIMEOUT);

    when(trendAnalysisService.getTaskStatus(anyList())).thenReturn(taskStatusMap);

    AnalysisStatus status = trendAnalysisState.getExecutionStatus();

    assertThat(status.name()).isEqualTo(AnalysisStatus.RETRY.name());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetExecutionStatus_queued() {
    String taskId = generateUuid();
    trendAnalysisState.setStatus(AnalysisStatus.RUNNING);
    trendAnalysisState.setWorkerTaskId(taskId);
    Map<String, ExecutionStatus> taskStatusMap = new HashMap<>();
    taskStatusMap.put(taskId, ExecutionStatus.QUEUED);

    when(trendAnalysisService.getTaskStatus(anyList())).thenReturn(taskStatusMap);

    AnalysisStatus status = trendAnalysisState.getExecutionStatus();

    assertThat(status.name()).isEqualTo(AnalysisStatus.RUNNING.name());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testHandleRerun() {
    trendAnalysisState.setRetryCount(2);
    trendAnalysisState.setStatus(AnalysisStatus.FAILED);

    trendAnalysisState.handleRerun();

    assertThat(trendAnalysisState.getStatus().name()).isEqualTo(AnalysisStatus.RUNNING.name());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testHandleRunning() {
    trendAnalysisState.setStatus(AnalysisStatus.RUNNING);

    trendAnalysisState.handleRunning();

    assertThat(trendAnalysisState.getStatus().name()).isEqualTo(AnalysisStatus.RUNNING.name());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testHandleSuccess() {
    AnalysisState state = trendAnalysisState.handleSuccess();
    assertThat(state.getStatus().name()).isEqualTo(AnalysisStatus.SUCCESS.name());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testHandleTransition() {
    AnalysisState state = trendAnalysisState.handleTransition();
    assertThat(state.getStatus().name()).isEqualTo(AnalysisStatus.SUCCESS.name());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testHandleRetry() {
    trendAnalysisState.setRetryCount(1);

    trendAnalysisState.handleRetry();

    assertThat(trendAnalysisState.getStatus().name()).isEqualTo(AnalysisStatus.RUNNING.name());
    assertThat(trendAnalysisState.getWorkerTaskId()).isNotNull();
    assertThat(trendAnalysisState.getRetryCount()).isEqualTo(2);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testHandleRetry_noMoreRetry() {
    trendAnalysisState.setRetryCount(2);

    trendAnalysisState.handleRetry();

    assertThat(trendAnalysisState.getStatus().name()).isEqualTo(AnalysisStatus.FAILED.name());
  }
}
