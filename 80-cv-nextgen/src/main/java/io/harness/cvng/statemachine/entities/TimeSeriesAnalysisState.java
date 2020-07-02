package io.harness.cvng.statemachine.entities;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.cvng.analysis.services.api.TimeSeriesAnalysisService;
import io.harness.cvng.statemachine.beans.AnalysisState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class TimeSeriesAnalysisState extends AnalysisState {
  @JsonIgnore @Inject private transient TimeSeriesAnalysisService timeSeriesAnalysisService;
  @Override
  public AnalysisState execute() {
    // TODO: Removed code for orchestration PR
    // schedule timeseries task via timeseries service
    timeSeriesAnalysisService.scheduleAnalysis(this.getInputs().getCvConfigId(), getInputs());
    this.setStatus(AnalysisStatus.RUNNING);
    logger.info("Executing timeseries analysis");
    return this;
  }

  @Override
  public AnalysisStatus getExecutionStatus() {
    // TODO: Removed code for orchestration PR
    return AnalysisStatus.SUCCESS;
  }

  @Override
  public AnalysisState handleRunning() {
    return this;
  }

  @Override
  public AnalysisState handleSuccess() {
    this.setStatus(AnalysisStatus.SUCCESS);
    return this;
  }

  @Override
  public AnalysisState handleTransition() {
    // time to transition to the next state. But for timeseries, this is the final state, so we will mark success.
    this.setStatus(AnalysisStatus.SUCCESS);
    return this;
  }

  @Override
  public AnalysisState handleRetry() {
    if (getRetryCount() >= getMaxRetry()) {
      this.setStatus(AnalysisStatus.FAILED);
    } else {
      setRetryCount(getRetryCount() + 1);
      // TODO: clean up some state.
      // TODO: Removed code for orchestration PR
      execute();
    }
    return this;
  }

  @Override
  public AnalysisState handleRerun() {
    // increment the retryCount without caring for the max
    // clean up state in underlying worker and then execute
    // TODO: Removed cleanup code for orchestration PR
    this.setRetryCount(getRetryCount() + 1);
    this.execute();
    return this;
  }
}
