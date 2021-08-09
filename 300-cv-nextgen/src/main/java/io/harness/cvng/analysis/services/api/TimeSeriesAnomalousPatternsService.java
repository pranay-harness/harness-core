package io.harness.cvng.analysis.services.api;

import io.harness.cvng.analysis.beans.ServiceGuardTimeSeriesAnalysisDTO;
import io.harness.cvng.analysis.beans.TimeSeriesAnomalies;

import java.util.List;
import java.util.Map;

public interface TimeSeriesAnomalousPatternsService {
  void saveAnomalousPatterns(ServiceGuardTimeSeriesAnalysisDTO analysis, String verificationTaskId);
  Map<String, Map<String, List<TimeSeriesAnomalies>>> getLongTermAnomalies(String verificationTaskId);
}
