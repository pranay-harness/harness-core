package io.harness.cvng.statemachine.services.api;

import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.analysis.services.api.TimeSeriesAnalysisService;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.entities.TimeSeriesAnalysisState;
import io.harness.cvng.statemachine.exception.AnalysisStateMachineException;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class TimeSeriesAnalysisStateExecutor<T extends TimeSeriesAnalysisState>
    extends AnalysisStateExecutor<T> {
  @Inject protected transient TimeSeriesAnalysisService timeSeriesAnalysisService;
  private String workerTaskId;

  @Override
  public AnalysisState execute(T analysisState) {
    List<String> taskIds = scheduleAnalysis(analysisState.getInputs());
    analysisState.setStatus(AnalysisStatus.RUNNING);

    if (taskIds != null && taskIds.size() == 1) {
      workerTaskId = taskIds.get(0);
    } else {
      throw new AnalysisStateMachineException(
          "Unknown number of worker tasks created in Timeseries Analysis State: " + taskIds);
    }
    log.info("Executing timeseries analysis");
    return analysisState;
  }

  protected abstract List<String> scheduleAnalysis(AnalysisInput analysisInput);

  @Override
  public AnalysisStatus getExecutionStatus(T analysisState) {
    if (!analysisState.getStatus().equals(AnalysisStatus.SUCCESS)) {
      Map<String, LearningEngineTask.ExecutionStatus> taskStatuses = timeSeriesAnalysisService.getTaskStatus(
          analysisState.getInputs().getVerificationTaskId(), new HashSet<>(Arrays.asList(workerTaskId)));
      LearningEngineTask.ExecutionStatus taskStatus = taskStatuses.get(workerTaskId);
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
  public AnalysisState handleRunning(T analysisState) {
    return analysisState;
  }

  @Override
  public AnalysisState handleSuccess(T analysisState) {
    analysisState.setStatus(AnalysisStatus.SUCCESS);
    return analysisState;
  }

  @Override
  public AnalysisState handleRetry(T analysisState) {
    if (analysisState.getRetryCount() >= getMaxRetry()) {
      analysisState.setStatus(AnalysisStatus.FAILED);
    } else {
      analysisState.setRetryCount(analysisState.getRetryCount() + 1);
      log.info("In TimeSeriesAnalysisState for Inputs {}, cleaning up worker task. Old taskID: {}",
          analysisState.getInputs(), workerTaskId);
      workerTaskId = null;
      analysisState.execute();
    }
    return analysisState;
  }

  @Override
  public AnalysisState handleTransition(T analysisState) {
    analysisState.setStatus(AnalysisStatus.SUCCESS);
    return analysisState;
  }

  @Override
  public AnalysisState handleRerun(T analysisState) {
    // increment the retryCount without caring for the max
    // clean up state in underlying worker and then execute

    analysisState.setRetryCount(analysisState.getRetryCount() + 1);
    log.info("In TimeSeriesAnalysisState for Inputs {}, cleaning up worker task. Old taskID: {}",
        analysisState.getInputs(), workerTaskId);
    workerTaskId = null;
    analysisState.execute();
    return analysisState;
  }
}
