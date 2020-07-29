package io.harness.cvng.statemachine.entities;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.cvng.analysis.beans.ExecutionStatus;
import io.harness.cvng.analysis.services.api.LogAnalysisService;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.exception.AnalysisStateMachineException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Data
@Builder
@Slf4j
public class ServiceGuardLogAnalysisState extends AnalysisState {
  @JsonIgnore @Inject private transient LogAnalysisService logAnalysisService;
  private String workerTaskId;

  @Override
  public AnalysisState execute() {
    List<String> taskIds = logAnalysisService.scheduleLogAnalysisTask(getInputs());
    this.setStatus(AnalysisStatus.RUNNING);

    if (taskIds != null && taskIds.size() == 1) {
      workerTaskId = taskIds.get(0);
    } else {
      throw new AnalysisStateMachineException(
          "Unknown number of worker tasks created in Timeseries Analysis State: " + taskIds);
    }
    logger.info("Executing service guard log analysis for {}", getInputs());
    return this;
  }

  @Override
  public AnalysisStatus getExecutionStatus() {
    if (!getStatus().equals(AnalysisStatus.SUCCESS)) {
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
    logger.info("In serviceguard log analysis for Inputs {}, cleaning up worker task. Old taskID: {}", getInputs(),
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
      logger.info("In serviceguard log analysis state, for Inputs {}, cleaning up worker task. Old taskID: {}",
          getInputs(), workerTaskId);
      workerTaskId = null;
      execute();
    }
    return this;
  }
}
