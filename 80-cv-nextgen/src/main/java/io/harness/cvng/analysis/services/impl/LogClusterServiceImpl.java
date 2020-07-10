package io.harness.cvng.analysis.services.impl;

import io.harness.cvng.analysis.beans.LogClusterDTO;
import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.analysis.services.api.LogClusterService;
import io.harness.cvng.statemachine.beans.AnalysisInput;

import java.time.Instant;
import java.util.List;

public class LogClusterServiceImpl implements LogClusterService {
  @Override
  public List<String> scheduleClusteringTasks(AnalysisInput input) {
    return null;
  }

  @Override
  public LogClusterDTO getDataForLogCluster(
      String cvConfig, Instant dataRecordInstant, String host, LogClusterLevel clusterLevel) {
    return null;
  }

  @Override
  public void saveClusteredData(LogClusterDTO logClusterDTO) {}
}
