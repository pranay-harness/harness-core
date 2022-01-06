/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
