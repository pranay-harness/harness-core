/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.instana;

import software.wings.metrics.MetricType;

import lombok.Data;
@Data
public class InstanaMetricTemplate {
  private String metricName;
  private String displayName;
  private MetricType metricType;
  private String aggregation;
}
