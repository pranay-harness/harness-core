package software.wings.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.math.Stats;

import org.junit.Test;

import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by mike@ on 4/10/17.
 */
public class MetricTest {
  @Test
  public void shouldTruncateByAge() {
    Metric<Integer> metric = new Metric<>("test", "test", MetricType.COUNT);
    metric.add(1, 10);
    metric.add(2, 20);
    metric.add(3, 30);
    metric.add(4, 40);
    metric.truncateByAge(25, TimeUnit.MILLISECONDS);
    assertThat(metric.getValues().size()).isEqualTo(3);
    assertThat(metric.getValues().firstEntry().getValue()).isEqualTo(2);
  }

  @Test
  public void shouldGenerateBucketsWithCorrectStats() {
    Metric<Integer> metric = new Metric<>("test", "test", MetricType.COUNT);
    metric.add(2, 10);
    metric.add(4, 20);
    metric.add(6, 30);
    metric.add(8, 40);
    TreeMap<Long, Stats> buckets = metric.generateBuckets(10, TimeUnit.MILLISECONDS);
    assertThat(buckets.size()).isEqualTo(4);
    assertThat(buckets.get(30l).mean()).isEqualTo(8);
    buckets = metric.generateBuckets(15, TimeUnit.MILLISECONDS);
    assertThat(buckets.size()).isEqualTo(3);
    assertThat(buckets.get(25l).mean()).isEqualTo(7);
  }
}
