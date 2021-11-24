package io.harness.cvng.statemachine.beans;

import static io.harness.cvng.analysis.CVAnalysisConstants.MAX_RETRIES;
import static io.harness.cvng.analysis.CVAnalysisConstants.TIMESERIES_ANALYSIS_RESOURCE;

import lombok.Data;

@Data
public abstract class AnalysisState {
  private AnalysisInput inputs;
  private AnalysisStatus status;
  private int retryCount;
  public abstract StateType getType();

  protected int getMaxRetry() {
    return MAX_RETRIES;
  }

  public enum StateType {
    CANARY_TIME_SERIES,
    DEPLOYMENT_LOG_ANALYSIS,
    SERVICE_GUARD_LOG_ANALYSIS,
    ACTIVITY_VERIFICATION,
    SERVICE_GUARD_TIME_SERIES,
    TEST_TIME_SERIES,
    DEPLOYMENT_LOG_CLUSTER,
    PRE_DEPLOYMENT_LOG_CLUSTER,
    SERVICE_GUARD_LOG_CLUSTER,
    SERVICE_GUARD_TREND_ANALYSIS

  }
}
