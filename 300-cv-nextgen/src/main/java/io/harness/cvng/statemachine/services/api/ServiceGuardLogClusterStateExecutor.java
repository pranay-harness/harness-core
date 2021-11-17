package io.harness.cvng.statemachine.services.api;

import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.entities.LogClusterState;
import io.harness.cvng.statemachine.entities.ServiceGuardLogAnalysisState;
import io.harness.cvng.statemachine.entities.ServiceGuardLogClusterState;
import io.harness.cvng.statemachine.exception.AnalysisStateMachineException;

import java.util.Collections;
import java.util.List;

public class ServiceGuardLogClusterStateExecutor extends LogClusterStateExecutor<ServiceGuardLogClusterState> {
  @Override
  protected List<String> scheduleAnalysis(AnalysisInput analysisInput, LogClusterState analysisState) {
    switch (analysisState.getClusterLevel()) {
      case L1:
        return logClusterService.scheduleL1ClusteringTasks(analysisInput);
      case L2:
        return logClusterService.scheduleServiceGuardL2ClusteringTask(analysisInput)
            .map(Collections::singletonList)
            .orElseGet(Collections::emptyList);
      default:
        throw new IllegalStateException("Invalid clusterLevel: " + analysisState.getClusterLevel());
    }
  }

  @Override
  public AnalysisState handleTransition(ServiceGuardLogClusterState analysisState) {
    analysisState.setStatus(AnalysisStatus.SUCCESS);
    switch (analysisState.getClusterLevel()) {
      case L1:
        ServiceGuardLogClusterState serviceGuardLogClusterState = ServiceGuardLogClusterState.builder().build();
        serviceGuardLogClusterState.setClusterLevel(LogClusterLevel.L2);
        serviceGuardLogClusterState.setInputs(analysisState.getInputs());
        serviceGuardLogClusterState.setStatus(AnalysisStatus.CREATED);
        return serviceGuardLogClusterState;
      case L2:
        ServiceGuardLogAnalysisState serviceGuardLogAnalysisState = ServiceGuardLogAnalysisState.builder().build();
        serviceGuardLogAnalysisState.setInputs(analysisState.getInputs());
        serviceGuardLogAnalysisState.setStatus(AnalysisStatus.CREATED);
        return serviceGuardLogAnalysisState;
      default:
        throw new AnalysisStateMachineException("Unknown cluster level in handleTransition "
            + "of ServiceGuardLogClusterState: " + analysisState.getClusterLevel());
    }
  }
}
