/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.billing.timeseries.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PodCountData {
  private String accountId;
  private String clusterId;
  private String nodeId;
  private long startTime;
  private long endTime;
  private long count;
}
