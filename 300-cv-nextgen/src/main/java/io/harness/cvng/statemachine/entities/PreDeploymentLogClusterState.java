package io.harness.cvng.statemachine.entities;

import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;

import com.google.common.base.Preconditions;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class PreDeploymentLogClusterState extends LogClusterState {
  @Builder
  public PreDeploymentLogClusterState(LogClusterLevel clusterLevel) {
    this.clusterLevel = clusterLevel;
  }

  @Override
  protected List<String> scheduleAnalysis(AnalysisInput analysisInput) {
    Preconditions.checkState(
        clusterLevel == LogClusterLevel.L1, "PreDeployment Log cluster state only does L1 clustering");
    return logClusterService.scheduleL1ClusteringTasks(analysisInput);
  }

  @Override
  public StateType getType() {
    return StateType.PRE_DEPLOYMENT_LOG_CLUSTER;
  }

  @Override
  public AnalysisState handleTransition() {
    this.setStatus(AnalysisStatus.SUCCESS);
    return this;
  }

  @Override
  public void handleFinalStatuses(AnalysisStatus finalStatus) {
    logClusterService.logDeploymentVerificationProgress(getInputs(), finalStatus, clusterLevel);
  }
}
