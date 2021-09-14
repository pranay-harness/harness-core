/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.dynatrace;

import software.wings.service.impl.dynatrace.DynaTraceTimeSeries.DynaTraceAggregationType;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

/**
 * Created by rsingh on 2/6/18.
 */
@Data
@Builder
public class DynaTraceMetricDataRequest {
  private String timeseriesId;
  private Set<String> entities;
  private DynaTraceAggregationType aggregationType;
  private Integer percentile;
  private long startTimestamp;
  private long endTimestamp;
}
