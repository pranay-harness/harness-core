package io.harness.cvng.analysis.services.api;

import io.harness.cvng.analysis.beans.LogClusterDTO;
import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.statemachine.beans.AnalysisInput;

import java.time.Instant;
import java.util.List;

public interface LogClusterService {
  List<String> scheduleClusteringTasks(AnalysisInput input);
  LogClusterDTO getDataForLogCluster(
      String cvConfig, Instant dataRecordInstant, String host, LogClusterLevel clusterLevel);
  void saveClusteredData(LogClusterDTO logClusterDTO);
}
