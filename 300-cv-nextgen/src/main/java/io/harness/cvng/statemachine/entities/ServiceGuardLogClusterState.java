package io.harness.cvng.statemachine.entities;

import io.harness.cvng.analysis.beans.LogClusterLevel;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ServiceGuardLogClusterState extends LogClusterState {
  @Builder
  public ServiceGuardLogClusterState(LogClusterLevel clusterLevel) {
    this.clusterLevel = clusterLevel;
  }
  private final StateType type = StateType.SERVICE_GUARD_LOG_CLUSTER;
}
