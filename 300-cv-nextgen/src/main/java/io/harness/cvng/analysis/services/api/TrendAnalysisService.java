package io.harness.cvng.analysis.services.api;

import io.harness.cvng.analysis.beans.ServiceGuardMetricAnalysisDTO;
import io.harness.cvng.analysis.entities.LearningEngineTask.ExecutionStatus;
import io.harness.cvng.core.beans.TimeSeriesMetricDefinition;
import io.harness.cvng.statemachine.beans.AnalysisInput;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface TrendAnalysisService {
  Map<String, ExecutionStatus> getTaskStatus(List<String> taskIds);

  String scheduleTrendAnalysisTask(AnalysisInput input);

  Map<String, Map<String, List<Double>>> getTestData(String verificationTaskId, Instant startTime, Instant endTime);

  void saveAnalysis(String taskId, ServiceGuardMetricAnalysisDTO analysis);

  List<TimeSeriesMetricDefinition> getTimeSeriesMetricDefinitions();
}
