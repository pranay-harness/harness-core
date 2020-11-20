package io.harness.cvng.statemachine.entities;

import com.google.common.base.Preconditions;

import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

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
  public AnalysisState handleTransition() {
    this.setStatus(AnalysisStatus.SUCCESS);
    return this;
  }

  @Override
  public void handleFinalStatuses(AnalysisStatus finalStatus) {
    logClusterService.logDeploymentVerificationProgress(getInputs(), finalStatus, clusterLevel);
  }
}
