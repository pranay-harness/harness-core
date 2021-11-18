package io.harness.cvng.statemachine.entities;

import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.analysis.entities.LearningEngineTask.ExecutionStatus;
import io.harness.cvng.statemachine.beans.AnalysisState;

import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
@Data
@Slf4j
public abstract class LogClusterState extends AnalysisState {
  protected LogClusterLevel clusterLevel;
  private Set<String> workerTaskIds;
  private Map<String, ExecutionStatus> workerTaskStatus;
}
