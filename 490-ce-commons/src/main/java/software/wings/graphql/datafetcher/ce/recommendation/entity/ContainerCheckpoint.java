/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.ce.recommendation.entity;

import io.harness.histogram.HistogramCheckpoint;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ContainerCheckpoint {
  Instant lastUpdateTime;
  HistogramCheckpoint cpuHistogram;
  HistogramCheckpoint memoryHistogram;

  Instant firstSampleStart;
  Instant lastSampleStart;
  int totalSamplesCount;

  long memoryPeak;
  Instant windowEnd;
  int version;
}
