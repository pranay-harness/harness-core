package io.harness.cvng.statemachine.entities;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
@Data
@Builder
@Slf4j
public class DeploymentLogAnalysisState extends LogAnalysisState {
  @Override
  public StateType getType() {
    return StateType.DEPLOYMENT_LOG_ANALYSIS;
  }
}
