/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.metrics.service.api;

import io.harness.metrics.beans.MetricConfiguration;

import java.util.List;

public interface MetricDefinitionInitializer {
  List<MetricConfiguration> getMetricConfiguration();
}
