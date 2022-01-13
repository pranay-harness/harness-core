/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
