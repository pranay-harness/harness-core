package io.harness.cvng.statemachine.services.api;

import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;

import static io.harness.cvng.analysis.CVAnalysisConstants.MAX_RETRIES;

public abstract class AnalysisStateExecutor {
  public abstract AnalysisState execute(AnalysisState analysisState);
  public abstract AnalysisStatus getExecutionStatus(AnalysisState analysisState);
  public abstract AnalysisState handleRerun(AnalysisState analysisState);
  public abstract AnalysisState handleRunning(AnalysisState analysisState);
  public abstract AnalysisState handleSuccess(AnalysisState analysisState);
  public abstract AnalysisState handleTransition(AnalysisState analysisState);
  public abstract AnalysisState handleRetry(AnalysisState analysisState);
  public void handleFinalStatuses(AnalysisState analysisState) {
    // no-op - designed to override
  }

  public AnalysisState handleFailure(AnalysisState analysisState) {
    analysisState.setStatus(AnalysisStatus.FAILED);
    return analysisState;
  }

   public AnalysisState handleTimeout(AnalysisState analysisState) {
    analysisState.setStatus(AnalysisStatus.TIMEOUT);
    return analysisState;
  }

  public int getMaxRetry() {
    return MAX_RETRIES;
  }
}
