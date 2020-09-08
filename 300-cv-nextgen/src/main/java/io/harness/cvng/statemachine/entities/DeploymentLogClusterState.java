package io.harness.cvng.statemachine.entities;

import static io.harness.cvng.analysis.beans.LogClusterLevel.L2;

import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.exception.AnalysisStateMachineException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
@Data
@Builder
@Slf4j
public class DeploymentLogClusterState extends LogClusterState {
  @Override
  protected List<String> scheduleAnalysis(AnalysisInput analysisInput) {
    return logClusterService.scheduleClusteringTasks(getInputs(), clusterLevel);
  }

  @Override
  public AnalysisState handleTransition() {
    this.setStatus(AnalysisStatus.SUCCESS);
    switch (clusterLevel) {
      case L1:
        DeploymentLogClusterState deploymentLogClusterState = DeploymentLogClusterState.builder().build();
        deploymentLogClusterState.setClusterLevel(L2);
        deploymentLogClusterState.setInputs(getInputs());
        deploymentLogClusterState.setStatus(AnalysisStatus.CREATED);
        return deploymentLogClusterState;
      case L2:
        throw new UnsupportedOperationException("create new analysis state here.");
      default:
        throw new AnalysisStateMachineException("Unknown cluster level in handleTransition "
            + "of ServiceGuardLogClusterState: " + clusterLevel);
    }
  }
}
