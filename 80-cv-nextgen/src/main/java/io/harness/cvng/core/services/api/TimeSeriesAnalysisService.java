package io.harness.cvng.core.services.api;

import io.harness.cvng.statemachine.beans.AnalysisInput;

/**
 * @author Praveen
 */
public interface TimeSeriesAnalysisService {
  void scheduleAnalysis(String cvConfigId, AnalysisInput input);
  void getTaskStatus(String cvConfigId, int analysisMinute);
}
