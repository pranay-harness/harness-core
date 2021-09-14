/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.analysis.beans;

import io.harness.cvng.analysis.entities.ClusteredLog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogClusterDTO {
  String verificationTaskId;
  long epochMinute;
  String host;
  String log;
  String clusterLabel;
  int clusterCount;

  public ClusteredLog toClusteredLog() {
    return ClusteredLog.builder()
        .verificationTaskId(verificationTaskId)
        .host(host)
        .clusterCount(clusterCount)
        .clusterLabel(clusterLabel)
        .log(log)
        .timestamp(Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(epochMinute)))
        .build();
  }
}
