/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.statemachine.entities;

import io.harness.cvng.analysis.entities.LearningEngineTask.ExecutionStatus;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.exception.AnalysisStateMachineException;

import java.util.Arrays;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@Slf4j
public class ServiceGuardLogAnalysisState extends LogAnalysisState {
  @Override
  protected String scheduleAnalysis(AnalysisInput analysisInput) {
    return getLogAnalysisService().scheduleServiceGuardLogAnalysisTask(analysisInput);
  }

  @Override
  public AnalysisState handleTransition() {
    this.setStatus(AnalysisStatus.SUCCESS);
    ServiceGuardTrendAnalysisState serviceGuardTrendAnalysisState = ServiceGuardTrendAnalysisState.builder().build();
    serviceGuardTrendAnalysisState.setInputs(getInputs());
    serviceGuardTrendAnalysisState.setStatus(AnalysisStatus.CREATED);
    return serviceGuardTrendAnalysisState;
  }

  @Override
  public AnalysisStatus getExecutionStatus() {
    if (getStatus() != AnalysisStatus.SUCCESS) {
      Map<String, ExecutionStatus> taskStatuses = logAnalysisService.getTaskStatus(Arrays.asList(workerTaskId));
      ExecutionStatus taskStatus = taskStatuses.get(workerTaskId);
      // This could be common code for all states.
      switch (taskStatus) {
        case SUCCESS:
          return AnalysisStatus.TRANSITION;
        case FAILED:
        case TIMEOUT:
          return AnalysisStatus.RETRY;
        case QUEUED:
        case RUNNING:
          return AnalysisStatus.RUNNING;
        default:
          throw new AnalysisStateMachineException(
              "Unknown worker state when executing service guard log analysis: " + taskStatus);
      }
    }
    return AnalysisStatus.TRANSITION;
  }
}
