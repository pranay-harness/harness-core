package io.harness.cvng.statemachine.entities;

import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
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
    return logClusterService.scheduleClusteringTasks(getInputs(), clusterLevel);
  }

  @Override
  public AnalysisState handleTransition() {
    this.setStatus(AnalysisStatus.SUCCESS);
    return this;
  }
}