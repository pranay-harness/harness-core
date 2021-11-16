package io.harness.cvng.statemachine.services.api;

import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.entities.DeploymentLogAnalysisState;

public class DeploymentLogAnalysisStateExecutor extends LogAnalysisStateExecutor<DeploymentLogAnalysisState> {
  @Override
  protected String scheduleAnalysis(AnalysisInput analysisInput) {
    return logAnalysisService.scheduleDeploymentLogAnalysisTask(analysisInput);
  }
}
