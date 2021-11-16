package io.harness.cvng.statemachine.services.api;

import static io.harness.cvng.analysis.CVAnalysisConstants.MAX_RETRIES;

import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;

public abstract class AnalysisStateExecutor<T extends AnalysisState> {
  public abstract AnalysisState execute(T analysisState);
  public abstract AnalysisStatus getExecutionStatus(T analysisState);
  public abstract AnalysisState handleRerun(T analysisState);
  public abstract AnalysisState handleRunning(T analysisState);
  public abstract AnalysisState handleSuccess(T analysisState);
  public abstract AnalysisState handleTransition(T analysisState);
  public abstract AnalysisState handleRetry(T analysisState);
  public void handleFinalStatuses(T analysisState) {
    // no-op - designed to override
  }

  public AnalysisState handleFailure(T analysisState) {
    analysisState.setStatus(AnalysisStatus.FAILED);
    return analysisState;
  }

  public AnalysisState handleTimeout(T analysisState) {
    analysisState.setStatus(AnalysisStatus.TIMEOUT);
    return analysisState;
  }

  public int getMaxRetry() {
    return MAX_RETRIES;
  }
}
