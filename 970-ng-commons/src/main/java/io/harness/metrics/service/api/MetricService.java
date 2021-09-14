/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.metrics.service.api;

import java.time.Duration;
import java.util.List;

public interface MetricService {
  void initializeMetrics();

  void initializeMetrics(List<MetricDefinitionInitializer> metricDefinitionInitializers);

  void recordMetric(String metricName, double value);

  void incCounter(String metricName);

  void recordDuration(String metricName, Duration duration);
}
