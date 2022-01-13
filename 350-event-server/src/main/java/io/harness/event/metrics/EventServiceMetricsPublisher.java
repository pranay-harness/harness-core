/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.event.metrics;

import io.harness.metrics.service.api.MetricService;
import io.harness.metrics.service.api.MetricsPublisher;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventServiceMetricsPublisher implements MetricsPublisher {
  @Inject private MetricService metricService;

  @Override
  public void recordMetrics() {
    // NOOP for now.
    // This method gets called once every minute. It can be used to capture instantaneous value of LastValue type
    // of metrics.
  }
}
