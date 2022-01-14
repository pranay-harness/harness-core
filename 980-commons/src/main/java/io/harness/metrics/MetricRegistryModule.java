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
