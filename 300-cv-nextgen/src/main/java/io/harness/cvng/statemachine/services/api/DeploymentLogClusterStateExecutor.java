package io.harness.cvng.statemachine.services.api;

import static io.harness.cvng.analysis.beans.LogClusterLevel.L2;

import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.entities.DeploymentLogAnalysisState;
import io.harness.cvng.statemachine.entities.DeploymentLogClusterState;
import io.harness.cvng.statemachine.exception.AnalysisStateMachineException;

import java.util.Collections;
import java.util.List;

public class DeploymentLogClusterStateExecutor extends LogClusterStateExecutor<DeploymentLogClusterState> {
  @Override
  protected List<String> scheduleAnalysis(AnalysisInput analysisInput) {
    switch (clusterLevel) {
      case L1:
        return logClusterService.scheduleL1ClusteringTasks(analysisInput);
      case L2:
        return logClusterService.scheduleDeploymentL2ClusteringTask(analysisInput)
            .map(Collections::singletonList)
            .orElseGet(Collections::emptyList);
      default:
        throw new IllegalStateException("Invalid clusterLevel: " + clusterLevel);
    }
  }

  @Override
  public AnalysisState handleTransition(DeploymentLogClusterState analysisState) {
    analysisState.setStatus(AnalysisStatus.SUCCESS);
    switch (clusterLevel) {
      case L1:
        DeploymentLogClusterState deploymentLogClusterState = DeploymentLogClusterState.builder().build();
        deploymentLogClusterState.setClusterLevel(L2);
        deploymentLogClusterState.setInputs(analysisState.getInputs());
        deploymentLogClusterState.setStatus(AnalysisStatus.CREATED);
        return deploymentLogClusterState;
      case L2:
        DeploymentLogAnalysisState deploymentLogAnalysisState = DeploymentLogAnalysisState.builder().build();
        deploymentLogAnalysisState.setInputs(analysisState.getInputs());
        deploymentLogAnalysisState.setStatus(AnalysisStatus.CREATED);
        return deploymentLogAnalysisState;
      default:
        throw new AnalysisStateMachineException("Unknown cluster level in handleTransition "
            + "of ServiceGuardLogClusterState: " + clusterLevel);
    }
  }
}
