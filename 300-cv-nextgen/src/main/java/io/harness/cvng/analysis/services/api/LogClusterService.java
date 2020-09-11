package io.harness.cvng.analysis.services.api;

import io.harness.cvng.analysis.beans.ExecutionStatus;
import io.harness.cvng.analysis.beans.LogClusterDTO;
import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.entities.AnalysisStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author praveensugavanam
 */
public interface LogClusterService {
  List<String> scheduleL1ClusteringTasks(AnalysisInput input);
  Optional<String> scheduleDeploymentL2ClusteringTask(AnalysisInput analysisInput);
  Optional<String> scheduleServiceGuardL2ClusteringTask(AnalysisInput analysisInput);
  Map<String, ExecutionStatus> getTaskStatus(Set<String> taskIds);
  List<LogClusterDTO> getDataForLogCluster(
      String verificationTaskId, Instant startTime, Instant endTime, String host, LogClusterLevel clusterLevel);
  List<LogClusterDTO> getClusteredLogData(
      String cvConfigId, Instant startTime, Instant endTime, LogClusterLevel clusterLevel);
  void saveClusteredData(List<LogClusterDTO> logClusterDTO, String verificationTaskId, Instant timestamp, String taskId,
      LogClusterLevel clusterLevel);
  void logDeploymentVerificationProgress(
      AnalysisInput analysisInput, AnalysisStatus analysisStatus, LogClusterLevel clusterLevel);
}
