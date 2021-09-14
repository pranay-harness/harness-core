/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.metrics;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import io.prometheus.client.CollectorRegistry;

/**
 * Created by Pranjal on 11/07/2018
 */
public class MetricRegistryModule extends AbstractModule {
  private HarnessMetricRegistry harnessMetricRegistry;

  private CollectorRegistry collectorRegistry = CollectorRegistry.defaultRegistry;

  public MetricRegistryModule(MetricRegistry metricRegistry) {
    harnessMetricRegistry = new HarnessMetricRegistry(metricRegistry, collectorRegistry);
  }

  @Override
  protected void configure() {
    bind(HarnessMetricRegistry.class).toInstance(harnessMetricRegistry);
  }
}
