package io.harness.cvng.statemachine.services.api;

import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.entities.LogClusterState;
import io.harness.cvng.statemachine.entities.PreDeploymentLogClusterState;

import com.google.common.base.Preconditions;
import java.util.List;

public class PreDeploymentLogClusterStateExecutor extends LogClusterStateExecutor<PreDeploymentLogClusterState> {
  @Override
  protected List<String> scheduleAnalysis(AnalysisInput analysisInput, LogClusterState analysisState) {
    Preconditions.checkState(analysisState.getClusterLevel() == LogClusterLevel.L1,
        "PreDeployment Log cluster state only does L1 clustering");
    return logClusterService.scheduleL1ClusteringTasks(analysisInput);
  }

  @Override
  public AnalysisState handleTransition(PreDeploymentLogClusterState analysisState) {
    analysisState.setStatus(AnalysisStatus.SUCCESS);
    return analysisState;
  }

  @Override
  public void handleFinalStatuses(PreDeploymentLogClusterState analysisState) {
    logClusterService.logDeploymentVerificationProgress(
        analysisState.getInputs(), analysisState.getStatus(), analysisState.getClusterLevel());
  }
}
