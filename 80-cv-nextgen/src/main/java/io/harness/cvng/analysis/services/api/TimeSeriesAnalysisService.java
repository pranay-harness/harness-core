package io.harness.cvng.analysis.services.api;

import io.harness.cvng.analysis.beans.ExecutionStatus;
import io.harness.cvng.analysis.beans.ServiceGuardMetricAnalysisDTO;
import io.harness.cvng.analysis.beans.TimeSeriesAnomalies;
import io.harness.cvng.analysis.entities.TimeSeriesCumulativeSums;
import io.harness.cvng.core.beans.TimeSeriesMetricDefinition;
import io.harness.cvng.statemachine.beans.AnalysisInput;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Praveen
 */
public interface TimeSeriesAnalysisService {
  List<String> scheduleAnalysis(String cvConfigId, AnalysisInput input);
  void getTaskStatus(String cvConfigId, int analysisMinute);
  ExecutionStatus getTaskStatus(String cvConfigId, Set<String> taskIds);
  Map<String, Map<String, TimeSeriesCumulativeSums.MetricSum>> getCumulativeSums(
      String cvConfigId, Instant startTime, Instant endTime);
  Map<String, Map<String, List<Double>>> getTestData(
      String cvConfigId, Instant epochStartMinute, Instant epochEndMinute);
  Map<String, Map<String, List<Double>>> getShortTermHistory(String cvConfigId);
  Map<String, Map<String, List<TimeSeriesAnomalies>>> getLongTermAnomalies(String cvConfigId);
  void saveAnalysis(ServiceGuardMetricAnalysisDTO analysis, String cvConfigId, String taskId);
  List<TimeSeriesMetricDefinition> getMetricTemplate(String cvConfigId);
}
