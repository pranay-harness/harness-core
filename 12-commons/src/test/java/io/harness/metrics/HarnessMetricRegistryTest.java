package io.harness.metrics;

import static io.harness.metrics.HarnessMetricRegistry.getAbsoluteMetricName;
import static io.harness.rule.OwnerRule.PRANJAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.MetricRegistry;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Created by Pranjal on 11/15/2018
 */
public class HarnessMetricRegistryTest extends CategoryTest {
  private HarnessMetricRegistry harnessMetricRegistry;

  @Before
  public void setup() {
    MetricRegistry metricRegistry = new MetricRegistry();
    CollectorRegistry collectorRegistry = CollectorRegistry.defaultRegistry;
    harnessMetricRegistry = new HarnessMetricRegistry(metricRegistry, collectorRegistry);
  }
  @Test
  @Owner(developers = PRANJAL)
  @Category(UnitTests.class)
  public void testGaugeMetricRegister() {
    String metricName = "data_collection_test_metric";
    harnessMetricRegistry.registerGaugeMetric(metricName, null, null);

    assertThat(harnessMetricRegistry.getNamesToCollectors().containsKey(getAbsoluteMetricName(metricName))).isTrue();
  }

  @Test
  @Owner(developers = PRANJAL)
  @Category(UnitTests.class)
  public void testGaugeMetricUpdate() {
    String metricName = "data_collection_test_metric1";
    double value = 100.0;
    harnessMetricRegistry.registerGaugeMetric(metricName, null, null);

    assertThat(harnessMetricRegistry.getNamesToCollectors().containsKey(getAbsoluteMetricName(metricName))).isTrue();

    harnessMetricRegistry.updateMetricValue(metricName, value);

    assertThat(value).isEqualTo(
        ((Gauge) harnessMetricRegistry.getNamesToCollectors().get(getAbsoluteMetricName(metricName))).get());
  }
}
