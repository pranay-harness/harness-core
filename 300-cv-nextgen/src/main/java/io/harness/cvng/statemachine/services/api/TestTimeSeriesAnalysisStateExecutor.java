package io.harness.cvng.statemachine.services.api;

import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.entities.TestTimeSeriesAnalysisState;

import java.util.List;

public class TestTimeSeriesAnalysisStateExecutor extends TimeSeriesAnalysisStateExecutor<TestTimeSeriesAnalysisState> {
  @Override
  protected List<String> scheduleAnalysis(AnalysisInput analysisInput) {
    return timeSeriesAnalysisService.scheduleTestVerificationTaskAnalysis(analysisInput);
  }

  @Override
  public void handleFinalStatuses(TestTimeSeriesAnalysisState analysisState) {
    timeSeriesAnalysisService.logDeploymentVerificationProgress(analysisState.getInputs(), analysisState.getStatus());
  }
}
