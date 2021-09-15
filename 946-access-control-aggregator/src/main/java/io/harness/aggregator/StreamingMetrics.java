/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.aggregator;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StreamingMetrics {
  long numberOfDisconnects;
  long numberOfPrimaryElections;
  long millisBehindSource;
  boolean connected;
  long millisSinceLastEvent;
  int queueTotalCapacity;
  int queueRemainingCapacity;
  String lastEvent;
  long totalNumberOfEventsSeen;
  long currentQueueSizeInBytes;
}
