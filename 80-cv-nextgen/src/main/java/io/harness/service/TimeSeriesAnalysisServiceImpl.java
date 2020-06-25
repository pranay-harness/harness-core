package io.harness.service;

import io.harness.service.intfc.TimeSeriesAnalysisService;
import io.harness.statemachine.entity.AnalysisInput;

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
