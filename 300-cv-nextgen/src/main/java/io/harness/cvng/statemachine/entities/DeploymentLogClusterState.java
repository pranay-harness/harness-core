package io.harness.cvng.statemachine.entities;

import static io.harness.cvng.statemachine.beans.AnalysisState.StateType.DEPLOYMENT_LOG_CLUSTER;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
@Data
@Builder
@Slf4j
public class DeploymentLogClusterState extends LogClusterState {
  private final StateType type = DEPLOYMENT_LOG_CLUSTER;
}
