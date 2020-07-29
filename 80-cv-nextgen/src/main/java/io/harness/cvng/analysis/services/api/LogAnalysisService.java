package io.harness.cvng.analysis.services.api;

import io.harness.cvng.analysis.beans.ExecutionStatus;
import io.harness.cvng.analysis.beans.LogAnalysisDTO;
import io.harness.cvng.analysis.beans.LogClusterDTO;
import io.harness.cvng.analysis.entities.LogAnalysisFrequencyPattern.FrequencyPattern;
import io.harness.cvng.statemachine.beans.AnalysisInput;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface LogAnalysisService {
  List<String> scheduleLogAnalysisTask(AnalysisInput input);
  Map<String, ExecutionStatus> getTaskStatus(List<String> taskIds);
  List<LogClusterDTO> getTestData(String cvConfigId, Instant analysisStartTime, Instant analysisEndTime);
  List<FrequencyPattern> getFrequencyPattern(String cvConfigId, Instant analysisStartTime, Instant analysisEndTime);
  void saveAnalysis(String cvConfigId, String taskId, Instant analysisStartTime, Instant analysisEndTime,
      LogAnalysisDTO analysisBody);
}
