/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans;

import io.harness.cvng.beans.TimeSeriesCustomThresholdActions;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.beans.TimeSeriesThresholdActionType;
import io.harness.cvng.beans.TimeSeriesThresholdComparisonType;
import io.harness.cvng.beans.TimeSeriesThresholdType;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TimeSeriesMetricDefinition {
  String metricName;
  TimeSeriesMetricType metricType;
  @Default String metricGroupName = "*";
  TimeSeriesThresholdActionType actionType;
  TimeSeriesThresholdComparisonType comparisonType;
  TimeSeriesCustomThresholdActions action;
  Integer occurrenceCount;
  TimeSeriesThresholdType thresholdType;
  Double value;
}
