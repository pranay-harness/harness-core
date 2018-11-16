package io.harness.registry;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import com.codahale.metrics.MetricRegistry;
import io.harness.exception.WingsException;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.dropwizard.DropwizardExports;
import lombok.NoArgsConstructor;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

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

  private final Map<String, Collector> namesToCollectors = new HashMap<>();

  // Default metric path for Custom Metrics
  // Any new custom metric that needs to be registered should have this path as prefix.
  // In the code
  private static final String DEFAULT_METRIC_PATH_PREFIX = "io_harness_custom_metric_";

  public HarnessMetricRegistry(MetricRegistry metricRegistry, CollectorRegistry collectorRegistry) {
    this.metricRegistry = metricRegistry;
    this.collectorRegistry = collectorRegistry;
    this.collectorRegistry.register(new DropwizardExports(metricRegistry));
  }

  public void registerGaugeMetric(String metricName, String[] labels, String doc) {
    String name = getAbsoluteMetricName(metricName);
    Gauge.Builder builder = Gauge.build().name(name).help(doc);
    if (labels != null) {
      builder.labelNames(labels);
    }
    if (doc != null) {
      builder.help(doc);
    } else {
      builder.help(metricName);
    }
    builder.register(collectorRegistry);
    namesToCollectors.put(name, builder.create());
  }

  public void recordGaugeInc(String metricName, String[] labelValues) {
    Gauge metric = (Gauge) namesToCollectors.get(getAbsoluteMetricName(metricName));
    if (labelValues != null) {
      metric.labels(labelValues).inc();
    } else {
      metric.inc();
    }
  }

  public void recordGaugeDec(String metricName, String[] labelValues) {
    Gauge metric = (Gauge) namesToCollectors.get(getAbsoluteMetricName(metricName));
    if (labelValues != null) {
      metric.labels(labelValues).dec();
    } else {
      metric.dec();
    }
  }

  public void updateMetricValue(String metricName, double value) {
    String name = getAbsoluteMetricName(metricName);

    Collector metric = namesToCollectors.get(name);
    if (metric != null) {
      String metricType = metric.getClass().getSimpleName();
      switch (metricType) {
        case "Gauge":
          ((Gauge) metric).set(value);
          break;
        default:
          throw new WingsException("Invalid Metric Type found " + metricType);
      }
    }
  }

  public Enumeration<MetricFamilySamples> getMetric() {
    return collectorRegistry.filteredMetricFamilySamples(new HashSet<>());
  }

  public static String getAbsoluteMetricName(String metricName) {
    if (!metricName.startsWith(DEFAULT_METRIC_PATH_PREFIX)) {
      return DEFAULT_METRIC_PATH_PREFIX + metricName;
    }
    return metricName;
  }

  @VisibleForTesting
  public Map<String, Collector> getNamesToCollectors() {
    return namesToCollectors;
  }
}
