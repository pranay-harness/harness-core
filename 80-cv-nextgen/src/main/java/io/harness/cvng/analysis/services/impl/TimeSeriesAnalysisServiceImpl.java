package io.harness.cvng.analysis.services.impl;

import io.harness.cvng.analysis.beans.ExecutionStatus;
import io.harness.cvng.analysis.beans.ServiceGuardMetricAnalysisDTO;
import io.harness.cvng.analysis.beans.TimeSeriesAnomalies;
import io.harness.cvng.analysis.entities.TimeSeriesCumulativeSums;
import io.harness.cvng.analysis.services.api.TimeSeriesAnalysisService;
import io.harness.cvng.core.beans.TimeSeriesMetricDefinition;
import io.harness.cvng.statemachine.beans.AnalysisInput;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TimeSeriesAnalysisServiceImpl implements TimeSeriesAnalysisService {
  @Override
  public List<String> scheduleAnalysis(String cvConfigId, AnalysisInput input) {
    return null;
  }

  @Override
  public void getTaskStatus(String cvConfigId, int analysisMinute) {}

  @Override
  public ExecutionStatus getTaskStatus(String cvConfigId, Set<String> taskIds) {
    return null;
  }

  @Override
  public Map<String, Map<String, TimeSeriesCumulativeSums.MetricSum>> getCumulativeSums(
      String cvConfigId, Instant startTime, Instant endTime) {
    return null;
  }

  @Override
  public Map<String, Map<String, List<Double>>> getTestData(
      String cvConfigId, Instant epochStartMinute, Instant epochEndMinute) {
    return null;
  }

  @Override
  public Map<String, Map<String, List<Double>>> getShortTermHistory(String cvConfigId) {
    return null;
  }

  @Override
  public Map<String, Map<String, List<TimeSeriesAnomalies>>> getLongTermAnomalies(String cvConfigId) {
    return null;
  }

  @Override
  public void saveAnalysis(ServiceGuardMetricAnalysisDTO analysis, String cvConfigId, String taskId) {}

  @Override
  public List<TimeSeriesMetricDefinition> getMetricTemplate(String cvConfigId) {
    return null;
  }
}
