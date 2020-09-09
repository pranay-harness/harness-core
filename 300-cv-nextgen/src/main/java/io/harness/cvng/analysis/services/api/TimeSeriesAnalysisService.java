package io.harness.cvng.analysis.services.api;

import io.harness.cvng.analysis.beans.DeploymentTimeSeriesAnalysisDTO;
import io.harness.cvng.analysis.beans.ExecutionStatus;
import io.harness.cvng.analysis.beans.ServiceGuardMetricAnalysisDTO;
import io.harness.cvng.analysis.beans.TimeSeriesAnomalies;
import io.harness.cvng.analysis.beans.TimeSeriesRecordDTO;
import io.harness.cvng.analysis.entities.TimeSeriesCumulativeSums;
import io.harness.cvng.core.beans.TimeSeriesMetricDefinition;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.entities.AnalysisStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TimeSeriesAnalysisService {
  List<String> scheduleServiceGuardAnalysis(AnalysisInput input);
  List<String> scheduleCanaryVerificationTaskAnalysis(AnalysisInput analysisInput);

  void logDeploymentVerificationProgress(AnalysisInput analysisInput, AnalysisStatus analysisStatus);

  Map<String, ExecutionStatus> getTaskStatus(String verificationTaskId, Set<String> taskIds);
  Map<String, Map<String, TimeSeriesCumulativeSums.MetricSum>> getCumulativeSums(
      String cvConfigId, Instant startTime, Instant endTime);
  Map<String, Map<String, List<Double>>> getTestData(
      String cvConfigId, Instant epochStartMinute, Instant epochEndMinute);
  Map<String, Map<String, List<Double>>> getShortTermHistory(String cvConfigId);
  Map<String, Map<String, List<TimeSeriesAnomalies>>> getLongTermAnomalies(String cvConfigId);
  List<TimeSeriesMetricDefinition> getMetricTemplate(String cvConfigId);
  List<TimeSeriesRecordDTO> getTimeSeriesRecordDTOs(String verificationTaskId, Instant startTime, Instant endTime);
  void saveAnalysis(String taskId, ServiceGuardMetricAnalysisDTO analysis);
  void saveAnalysis(String taskId, DeploymentTimeSeriesAnalysisDTO analysis);
}
