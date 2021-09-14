/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.statemachine.entities;

import static io.harness.cvng.analysis.beans.LogClusterLevel.L2;

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
@Builder
@Slf4j
public class DeploymentLogClusterState extends LogClusterState {
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
        DeploymentLogAnalysisState deploymentLogAnalysisState = DeploymentLogAnalysisState.builder().build();
        deploymentLogAnalysisState.setInputs(getInputs());
        deploymentLogAnalysisState.setStatus(AnalysisStatus.CREATED);
        return deploymentLogAnalysisState;
      default:
        throw new AnalysisStateMachineException("Unknown cluster level in handleTransition "
            + "of ServiceGuardLogClusterState: " + clusterLevel);
    }
  }

  @Override
  public void handleFinalStatuses(AnalysisStatus finalStatus) {
    logClusterService.logDeploymentVerificationProgress(getInputs(), finalStatus, clusterLevel);
  }
}
