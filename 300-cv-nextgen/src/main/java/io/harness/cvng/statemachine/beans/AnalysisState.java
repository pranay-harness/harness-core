package io.harness.cvng.statemachine.beans;

import static io.harness.cvng.analysis.CVAnalysisConstants.MAX_RETRIES;
import static io.harness.cvng.analysis.CVAnalysisConstants.TIMESERIES_ANALYSIS_RESOURCE;

import lombok.Data;

@Data
public abstract class AnalysisState {
  private AnalysisInput inputs;
  private AnalysisStatus status;
  private int retryCount;

  public abstract AnalysisState execute();
  public abstract AnalysisStatus getExecutionStatus();
  public abstract AnalysisState handleRerun();
  public abstract AnalysisState handleRunning();
  public abstract AnalysisState handleSuccess();
  public abstract AnalysisState handleTransition();
  public abstract AnalysisState handleRetry();
  public void handleFinalStatuses(AnalysisStatus finalStatus) {
    // no-op - designed to override
  }

  public AnalysisState handleFailure() {
    this.setStatus(AnalysisStatus.FAILED);
    return this;
  }

  public AnalysisState handleTimeout() {
    this.setStatus(AnalysisStatus.TIMEOUT);
    return this;
  }

  protected int getMaxRetry() {
    return MAX_RETRIES;
  }
}
