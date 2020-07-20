package io.harness.cvng.analysis.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.cvng.analysis.entities.ClusteredLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Value
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogClusterDTO {
  String cvConfigId;
  long epochMinute;
  String host;
  String log;
  String clusterLabel;
  int clusterCount;

  public ClusteredLog toClusteredLog() {
    return ClusteredLog.builder()
        .cvConfigId(cvConfigId)
        .host(host)
        .clusterCount(clusterCount)
        .clusterLabel(clusterLabel)
        .log(log)
        .timestamp(Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(epochMinute)))
        .build();
  }
}
