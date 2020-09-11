package io.harness.cvng.analysis.services.api;

import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO;
import io.harness.cvng.analysis.beans.ExecutionStatus;
import io.harness.cvng.analysis.beans.LogAnalysisDTO;
import io.harness.cvng.analysis.beans.LogClusterDTO;
import io.harness.cvng.analysis.entities.LogAnalysisCluster;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.entities.AnalysisStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface LogAnalysisService {
  String scheduleServiceGuardLogAnalysisTask(AnalysisInput input);
  String scheduleDeploymentLogAnalysisTask(AnalysisInput analysisInput);
  Map<String, ExecutionStatus> getTaskStatus(List<String> taskIds);
  List<LogClusterDTO> getTestData(String verificationTaskId, Instant analysisStartTime, Instant analysisEndTime);
  List<LogAnalysisCluster> getPreviousAnalysis(String cvConfigId, Instant analysisStartTime, Instant analysisEndTime);
  void saveAnalysis(String cvConfigId, String taskId, Instant analysisStartTime, Instant analysisEndTime,
      LogAnalysisDTO analysisBody);
  void saveAnalysis(String learningEngineTaskId, DeploymentLogAnalysisDTO deploymentLogAnalysisDTO);

  void logDeploymentVerificationProgress(AnalysisInput inputs, AnalysisStatus finalStatus);
}
