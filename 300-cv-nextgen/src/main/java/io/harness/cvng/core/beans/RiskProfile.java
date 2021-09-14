/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.beans.TimeSeriesThresholdType;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RiskProfile {
  private CVMonitoringCategory category;
  private TimeSeriesMetricType metricType;
  List<TimeSeriesThresholdType> thresholdTypes;
}
