/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.statemachine.entities;

import io.harness.cvng.analysis.entities.LearningEngineTask.ExecutionStatus;
import io.harness.cvng.analysis.services.api.TrendAnalysisService;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.exception.AnalysisStateMachineException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class ServiceGuardTrendAnalysisState extends AnalysisState {
  @JsonIgnore @Inject private transient TrendAnalysisService trendAnalysisService;
  private String workerTaskId;

  @Override
  public AnalysisState execute() {
    workerTaskId = trendAnalysisService.scheduleTrendAnalysisTask(getInputs());
    this.setStatus(AnalysisStatus.RUNNING);
    log.info("Executing service guard trend analysis for {}", getInputs());
    return this;
  }

  @Override
  public AnalysisStatus getExecutionStatus() {
    if (getStatus() != AnalysisStatus.SUCCESS) {
      Map<String, ExecutionStatus> taskStatuses =
          trendAnalysisService.getTaskStatus(Collections.singletonList(workerTaskId));
      ExecutionStatus taskStatus = taskStatuses.get(workerTaskId);
      // This could be common code for all states.
      switch (taskStatus) {
        case SUCCESS:
          return AnalysisStatus.SUCCESS;
        case FAILED:
        case TIMEOUT:
          return AnalysisStatus.RETRY;
        case QUEUED:
        case RUNNING:
          return AnalysisStatus.RUNNING;
        default:
          throw new AnalysisStateMachineException(
              "Unknown worker state when executing service guard trend analysis: " + taskStatus);
      }
    }
    return AnalysisStatus.SUCCESS;
  }

  @Override
  public AnalysisState handleRerun() {
    // increment the retryCount without caring for the max
    // clean up state in underlying worker and then execute
    this.setRetryCount(getRetryCount() + 1);
    log.info("In service guard trend analysis for Inputs {}, cleaning up worker task. Old taskID: {}", getInputs(),
        workerTaskId);
    workerTaskId = null;
    this.execute();
    return this;
  }

  @Override
  public AnalysisState handleRunning() {
    return this;
  }

  @Override
  public AnalysisState handleSuccess() {
    this.setStatus(AnalysisStatus.SUCCESS);
    return this;
  }

  @Override
  public AnalysisState handleTransition() {
    this.setStatus(AnalysisStatus.SUCCESS);
    return this;
  }

  @Override
  public AnalysisState handleRetry() {
    if (getRetryCount() >= getMaxRetry()) {
      this.setStatus(AnalysisStatus.FAILED);
    } else {
      setRetryCount(getRetryCount() + 1);
      log.info("In service guard trend analysis state, for Inputs {}, cleaning up worker task. Old taskID: {}",
          getInputs(), workerTaskId);
      workerTaskId = null;
      execute();
    }
    return this;
  }
}
