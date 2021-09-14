/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.statemachine.entities;

import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.exception.AnalysisStateMachineException;

import java.util.Collections;
import java.util.List;
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
  @Override
  protected List<String> scheduleAnalysis(AnalysisInput analysisInput) {
    switch (clusterLevel) {
      case L1:
        return logClusterService.scheduleL1ClusteringTasks(analysisInput);
      case L2:
        return logClusterService.scheduleServiceGuardL2ClusteringTask(analysisInput)
            .map(Collections::singletonList)
            .orElseGet(Collections::emptyList);
      default:
        throw new IllegalStateException("Invalid clusterLevel: " + clusterLevel);
    }
  }

  @Override
  public AnalysisState handleTransition() {
    this.setStatus(AnalysisStatus.SUCCESS);
    switch (clusterLevel) {
      case L1:
        ServiceGuardLogClusterState serviceGuardLogClusterState = ServiceGuardLogClusterState.builder().build();
        serviceGuardLogClusterState.setClusterLevel(LogClusterLevel.L2);
        serviceGuardLogClusterState.setInputs(getInputs());
        serviceGuardLogClusterState.setStatus(AnalysisStatus.CREATED);
        return serviceGuardLogClusterState;
      case L2:
        ServiceGuardLogAnalysisState serviceGuardLogAnalysisState = ServiceGuardLogAnalysisState.builder().build();
        serviceGuardLogAnalysisState.setInputs(getInputs());
        serviceGuardLogAnalysisState.setStatus(AnalysisStatus.CREATED);
        return serviceGuardLogAnalysisState;
      default:
        throw new AnalysisStateMachineException("Unknown cluster level in handleTransition "
            + "of ServiceGuardLogClusterState: " + clusterLevel);
    }
  }
}
