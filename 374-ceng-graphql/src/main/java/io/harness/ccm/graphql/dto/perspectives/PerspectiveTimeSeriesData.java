/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.graphql.dto.perspectives;

import io.harness.ccm.graphql.dto.common.TimeSeriesDataPoints;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PerspectiveTimeSeriesData {
  List<TimeSeriesDataPoints> stats;
  List<TimeSeriesDataPoints> cpuUtilValues;
  List<TimeSeriesDataPoints> memoryUtilValues;
  List<TimeSeriesDataPoints> cpuRequest;
  List<TimeSeriesDataPoints> cpuLimit;
  List<TimeSeriesDataPoints> memoryRequest;
  List<TimeSeriesDataPoints> memoryLimit;
}
