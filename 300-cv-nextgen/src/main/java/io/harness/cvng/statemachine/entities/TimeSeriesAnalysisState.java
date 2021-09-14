/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.statemachine.entities;

import io.harness.cvng.analysis.entities.LearningEngineTask.ExecutionStatus;
import io.harness.cvng.analysis.services.api.TimeSeriesAnalysisService;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.exception.AnalysisStateMachineException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Slf4j
public abstract class TimeSeriesAnalysisState extends AnalysisState {
  @JsonIgnore @Inject protected transient TimeSeriesAnalysisService timeSeriesAnalysisService;
  private String workerTaskId;
  @Override
  public AnalysisState execute() {
    List<String> taskIds = scheduleAnalysis(getInputs());
    this.setStatus(AnalysisStatus.RUNNING);

    if (taskIds != null && taskIds.size() == 1) {
      workerTaskId = taskIds.get(0);
    } else {
      throw new AnalysisStateMachineException(
          "Unknown number of worker tasks created in Timeseries Analysis State: " + taskIds);
    }
    log.info("Executing timeseries analysis");
    return this;
  }

  protected abstract List<String> scheduleAnalysis(AnalysisInput analysisInput);

  @Override
  public AnalysisStatus getExecutionStatus() {
    if (!getStatus().equals(AnalysisStatus.SUCCESS)) {
      Map<String, ExecutionStatus> taskStatuses = timeSeriesAnalysisService.getTaskStatus(
          getInputs().getVerificationTaskId(), new HashSet<>(Arrays.asList(workerTaskId)));
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
              "Unknown worker state when executing timeseries analysis: " + taskStatus);
      }
    }
    return AnalysisStatus.SUCCESS;
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
    // time to transition to the next state. But for timeseries, this is the final state, so we will mark success.
    this.setStatus(AnalysisStatus.SUCCESS);
    return this;
  }

  @Override
  public AnalysisState handleRetry() {
    if (getRetryCount() >= getMaxRetry()) {
      this.setStatus(AnalysisStatus.FAILED);
    } else {
      setRetryCount(getRetryCount() + 1);
      log.info("In TimeSeriesAnalysisState for Inputs {}, cleaning up worker task. Old taskID: {}", getInputs(),
          workerTaskId);
      workerTaskId = null;
      execute();
    }
    return this;
  }

  @Override
  public AnalysisState handleRerun() {
    // increment the retryCount without caring for the max
    // clean up state in underlying worker and then execute

    this.setRetryCount(getRetryCount() + 1);
    log.info(
        "In TimeSeriesAnalysisState for Inputs {}, cleaning up worker task. Old taskID: {}", getInputs(), workerTaskId);
    workerTaskId = null;
    this.execute();
    return this;
  }
}
