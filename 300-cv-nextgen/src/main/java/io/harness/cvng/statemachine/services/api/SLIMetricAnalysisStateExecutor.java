package io.harness.cvng.statemachine.services.api;

import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.entities.SLIMetricAnalysisState;

public class SLIMetricAnalysisStateExecutor extends AnalysisStateExecutor<SLIMetricAnalysisState> {
  @Override
  public AnalysisState execute(SLIMetricAnalysisState analysisState) {
    return null;
  }

  @Override
  public AnalysisStatus getExecutionStatus(SLIMetricAnalysisState analysisState) {
    return null;
  }

  @Override
  public AnalysisState handleRerun(SLIMetricAnalysisState analysisState) {
    return null;
  }

  @Override
  public AnalysisState handleRunning(SLIMetricAnalysisState analysisState) {
    return null;
  }

  @Override
  public AnalysisState handleSuccess(SLIMetricAnalysisState analysisState) {
    return null;
  }

  @Override
  public AnalysisState handleTransition(SLIMetricAnalysisState analysisState) {
    return null;
  }

  @Override
  public AnalysisState handleRetry(SLIMetricAnalysisState analysisState) {
    return null;
  }
}
