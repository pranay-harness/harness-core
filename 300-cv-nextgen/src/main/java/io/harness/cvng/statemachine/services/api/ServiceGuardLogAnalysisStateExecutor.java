package io.harness.cvng.statemachine.services.api;

import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.entities.ServiceGuardLogAnalysisState;
import io.harness.cvng.statemachine.entities.ServiceGuardTrendAnalysisState;
import io.harness.cvng.statemachine.exception.AnalysisStateMachineException;

import java.util.Arrays;
import java.util.Map;

public class ServiceGuardLogAnalysisStateExecutor extends LogAnalysisStateExecutor<ServiceGuardLogAnalysisState> {
  @Override
  protected String scheduleAnalysis(AnalysisInput analysisInput) {
    return logAnalysisService.scheduleServiceGuardLogAnalysisTask(analysisInput);
  }

  @Override
  public AnalysisState handleTransition(ServiceGuardLogAnalysisState serviceGuardLogAnalysisState) {
    serviceGuardLogAnalysisState.setStatus(AnalysisStatus.SUCCESS);
    ServiceGuardTrendAnalysisState serviceGuardTrendAnalysisState = ServiceGuardTrendAnalysisState.builder().build();
    serviceGuardTrendAnalysisState.setInputs(serviceGuardLogAnalysisState.getInputs());
    serviceGuardTrendAnalysisState.setStatus(AnalysisStatus.CREATED);
    return serviceGuardTrendAnalysisState;
  }

  @Override
  public AnalysisStatus getExecutionStatus(ServiceGuardLogAnalysisState serviceGuardLogAnalysisState) {
    if (serviceGuardLogAnalysisState.getStatus() != AnalysisStatus.SUCCESS) {
      Map<String, LearningEngineTask.ExecutionStatus> taskStatuses =
          logAnalysisService.getTaskStatus(Arrays.asList(workerTaskId));
      LearningEngineTask.ExecutionStatus taskStatus = taskStatuses.get(workerTaskId);
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
