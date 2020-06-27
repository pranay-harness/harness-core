package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.services.api.TimeSeriesAnalysisService;
import io.harness.cvng.statemachine.beans.AnalysisInput;

/**
 * @author Praveen
 */
public class TimeSeriesAnalysisServiceImpl implements TimeSeriesAnalysisService {
  @Override
  public void scheduleAnalysis(String cvConfigId, AnalysisInput analysisInput) {
    // TODO: Removed code for orchestration PR
  }

  @Override
  public void getTaskStatus(String cvConfigId, int analysisMinute) {
    // TODO: Removed code for orchestration PR
  }
}
