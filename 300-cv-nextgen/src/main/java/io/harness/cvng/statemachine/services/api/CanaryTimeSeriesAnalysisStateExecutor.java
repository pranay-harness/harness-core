package io.harness.cvng.statemachine.services.api;

import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.entities.CanaryTimeSeriesAnalysisState;

import java.util.List;

public class CanaryTimeSeriesAnalysisStateExecutor
    extends TimeSeriesAnalysisStateExecutor<CanaryTimeSeriesAnalysisState> {
  @Override
  protected List<String> scheduleAnalysis(AnalysisInput analysisInput) {
    return timeSeriesAnalysisService.scheduleCanaryVerificationTaskAnalysis(analysisInput);
  }

  @Override
  public void handleFinalStatuses(CanaryTimeSeriesAnalysisState analysisState) {
    timeSeriesAnalysisService.logDeploymentVerificationProgress(analysisState.getInputs(), analysisState.getStatus());
  }
}
