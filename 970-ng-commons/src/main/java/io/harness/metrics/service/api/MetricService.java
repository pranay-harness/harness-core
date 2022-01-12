/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
