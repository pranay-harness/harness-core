package io.harness.registry;

import com.google.inject.Singleton;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import io.harness.exception.WingsException;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import lombok.NoArgsConstructor;

import java.util.Enumeration;
import java.util.HashSet;

/**
 * Harness Metric Registry is a custom Metric Registry build on the top of CodeHale MetricRegistry
 * Any Codehale metric that needs to be projected has to be registered here
 * This metric Registry supports all kind of Metrics by DropWizard
 * To register custom Metric use specific register method.
 * Or if want to register metrics by annotation that can be done directly by annotations.
 *
 * Created by Pranjal on 11/01/2018
 */
@NoArgsConstructor
@Singleton
public class HarnessMetricRegistry {
  // DropWizard metric registry that stores the metrics
  private MetricRegistry metricRegistry;

  // Prometheus Collector Registry used for exposing metrics to prometheus by rest endpoint
  private CollectorRegistry collectorRegistry;

  // Default metric path for Custom Metrics
  // Any new custom metric that needs to be registered should have this path as prefix.
  // In the code
  private final String DEFAULT_METRIC_PATH_PREFIX = "io_harness_custom_metric_";

  public HarnessMetricRegistry(MetricRegistry metricRegistry, CollectorRegistry collectorRegistry) {
    this.metricRegistry = metricRegistry;
    this.collectorRegistry = collectorRegistry;
    this.collectorRegistry.register(new DropwizardExports(metricRegistry));
  }

  public void registerGaugeMetric(String metricName, CustomGauge metric) {
    String name = getAbsoluteMetricName(metricName);
    metricRegistry.register(name, metric);
  }

  public void registerMeterMetric(String metricName, Meter meter) {
    String name = getAbsoluteMetricName(metricName);
    metricRegistry.register(name, meter);
  }

  public void registerHistogramMetric(String metricName) {
    String name = getAbsoluteMetricName(metricName);
    metricRegistry.histogram(name);
  }

  public void updateMetricValue(String metricName, long value) {
    String name = getAbsoluteMetricName(metricName);
    Metric metric = metricRegistry.getMetrics().get(name);
    if (metric != null) {
      String metricType = metric.getClass().getSimpleName();
      switch (metricType) {
        case "CustomGauge":
          ((CustomGauge) metric).setValue(value);
          break;
        case "Meter":
          ((Meter) metric).mark(value);
          break;
        case "Histogram":
          ((Histogram) metric).update(value);
          break;
        default:
          throw new WingsException("Invalid Metric Type found " + metricType);
      }
    }
  }

  public Enumeration<MetricFamilySamples> getMetric() {
    return collectorRegistry.filteredMetricFamilySamples(new HashSet<>());
  }

  private String getAbsoluteMetricName(String metricName) {
    if (!metricName.startsWith(DEFAULT_METRIC_PATH_PREFIX)) {
      return DEFAULT_METRIC_PATH_PREFIX + metricName;
    }
    return metricName;
  }
}
