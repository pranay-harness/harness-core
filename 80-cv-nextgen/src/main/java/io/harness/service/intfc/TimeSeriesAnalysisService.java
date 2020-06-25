package io.harness.service.intfc;

import io.harness.statemachine.entity.AnalysisInput;

/**
 * @author Praveen
 */
public interface TimeSeriesAnalysisService {
  void scheduleAnalysis(String cvConfigId, AnalysisInput input);
  void getTaskStatus(String cvConfigId, int analysisMinute);
}
