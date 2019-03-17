package io.harness.metrics;

import static io.harness.metrics.HarnessMetricRegistry.getAbsoluteMetricName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.codahale.metrics.MetricRegistry;
import io.harness.category.element.UnitTests;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Created by Pranjal on 11/15/2018
 */
public class HarnessMetricRegistryTest {
  private HarnessMetricRegistry harnessMetricRegistry;

  @Before
  public void setup() {
    MetricRegistry metricRegistry = new MetricRegistry();
    CollectorRegistry collectorRegistry = CollectorRegistry.defaultRegistry;
    harnessMetricRegistry = new HarnessMetricRegistry(metricRegistry, collectorRegistry);
  }
  @Test
  @Category(UnitTests.class)
  public void testGaugeMetricRegister() {
    String metricName = "data_collection_test_metric";
    harnessMetricRegistry.registerGaugeMetric(metricName, null, null);

    assertTrue(harnessMetricRegistry.getNamesToCollectors().containsKey(getAbsoluteMetricName(metricName)));
  }

  @Test
  @Category(UnitTests.class)
  public void testGaugeMetricUpdate() {
    String metricName = "data_collection_test_metric1";
    double value = 100.0;
    harnessMetricRegistry.registerGaugeMetric(metricName, null, null);

    assertTrue(harnessMetricRegistry.getNamesToCollectors().containsKey(getAbsoluteMetricName(metricName)));

    harnessMetricRegistry.updateMetricValue(metricName, value);

    assertEquals(
        ((Gauge) harnessMetricRegistry.getNamesToCollectors().get(getAbsoluteMetricName(metricName))).get(), value, 0);
  }
}
