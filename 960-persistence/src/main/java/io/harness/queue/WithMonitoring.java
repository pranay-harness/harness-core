/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.queue;

import io.harness.metrics.ThreadAutoLogContext;

public interface WithMonitoring {
  ThreadAutoLogContext metricContext();

  String getMetricPrefix();

  Long getCreatedAt();

  boolean isMonitoringEnabled();
}
