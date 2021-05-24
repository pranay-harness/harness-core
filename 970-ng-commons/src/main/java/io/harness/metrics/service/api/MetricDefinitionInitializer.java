package io.harness.metrics.service.api;

import io.harness.metrics.beans.MetricConfiguration;

import java.util.List;

public interface MetricDefinitionInitializer {
  List<MetricConfiguration> getMetricConfiguration();
}
