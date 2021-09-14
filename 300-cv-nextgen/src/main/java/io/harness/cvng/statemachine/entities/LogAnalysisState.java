/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.statemachine.entities;

import io.harness.cvng.analysis.entities.LearningEngineTask.ExecutionStatus;
import io.harness.cvng.analysis.services.api.LogAnalysisService;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.exception.AnalysisStateMachineException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public abstract class LogAnalysisState extends AnalysisState {
  @JsonIgnore @Inject protected transient LogAnalysisService logAnalysisService;
  protected String workerTaskId;

  @Override
  public AnalysisState execute() {
    workerTaskId = scheduleAnalysis(getInputs());
    Preconditions.checkNotNull(workerTaskId, "workerId can not be null");
    this.setStatus(AnalysisStatus.RUNNING);
    log.info("Executing service guard log analysis for {}", getInputs());
    return this;
  }

  protected abstract String scheduleAnalysis(AnalysisInput analysisInput);

  @Override
  public AnalysisStatus getExecutionStatus() {
    if (getStatus() != AnalysisStatus.SUCCESS) {
      Map<String, ExecutionStatus> taskStatuses = logAnalysisService.getTaskStatus(Arrays.asList(workerTaskId));
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
              "Unknown worker state when executing service guard log analysis: " + taskStatus);
      }
    }
    return AnalysisStatus.SUCCESS;
  }

  @Override
  public AnalysisState handleRerun() {
    // increment the retryCount without caring for the max
    // clean up state in underlying worker and then execute

    this.setRetryCount(getRetryCount() + 1);
    log.info("In serviceguard log analysis for Inputs {}, cleaning up worker task. Old taskID: {}", getInputs(),
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
      log.info("In serviceguard log analysis state, for Inputs {}, cleaning up worker task. Old taskID: {}",
          getInputs(), workerTaskId);
      workerTaskId = null;
      execute();
    }
    return this;
  }
}
