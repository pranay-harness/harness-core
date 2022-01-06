/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
