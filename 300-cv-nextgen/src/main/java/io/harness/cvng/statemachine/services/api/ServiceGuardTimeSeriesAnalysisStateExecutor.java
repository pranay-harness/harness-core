package io.harness.cvng.statemachine.services.api;

import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.entities.ServiceGuardTimeSeriesAnalysisState;

import java.util.List;

public class ServiceGuardTimeSeriesAnalysisStateExecutor
    extends TimeSeriesAnalysisStateExecutor<ServiceGuardTimeSeriesAnalysisState> {
  @Override
  protected List<String> scheduleAnalysis(AnalysisInput analysisInput) {
    return timeSeriesAnalysisService.scheduleServiceGuardAnalysis(analysisInput);
  }
}
