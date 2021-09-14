/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.config.k8s.recommendation;

import static software.wings.graphql.datafetcher.ce.recommendation.entity.RecommenderUtils.newCpuHistogramV2;

import io.harness.histogram.Histogram;

import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerCheckpoint;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * ContainerState maintains the live structures for each container, such as cpu & mem histograms and the various
 * timestamps. In v2, we store only data for a single day.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContainerStateV2 {
  Instant lastUpdateTime;
  Histogram cpuHistogram = newCpuHistogramV2();

  // Below are all based on cpu
  private Instant firstSampleStart;
  private Instant lastSampleStart;
  private int totalSamplesCount;

  // only single peak required for a day - no need of histogram
  long memoryPeak;
  int version;

  static ContainerStateV2 fromCheckpoint(ContainerCheckpoint containerCheckpoint) {
    ContainerStateV2 containerState = new ContainerStateV2();
    containerState.setLastUpdateTime(containerCheckpoint.getLastUpdateTime());
    containerState.cpuHistogram.loadFromCheckPoint(containerCheckpoint.getCpuHistogram());
    containerState.setFirstSampleStart(containerCheckpoint.getFirstSampleStart());
    containerState.setLastSampleStart(containerCheckpoint.getLastSampleStart());
    containerState.setTotalSamplesCount(containerCheckpoint.getTotalSamplesCount());
    containerState.setMemoryPeak(containerCheckpoint.getMemoryPeak());
    containerState.setVersion(containerCheckpoint.getVersion());
    return containerState;
  }

  ContainerCheckpoint toContainerCheckpoint() {
    return ContainerCheckpoint.builder()
        .lastUpdateTime(lastUpdateTime)
        .cpuHistogram(cpuHistogram.saveToCheckpoint())
        .firstSampleStart(firstSampleStart)
        .lastSampleStart(lastSampleStart)
        .totalSamplesCount(totalSamplesCount)
        .memoryPeak(memoryPeak)
        .version(version)
        .build();
  }
}
