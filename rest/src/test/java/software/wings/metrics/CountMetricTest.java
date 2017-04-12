package software.wings.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created by mike@ on 4/11/17.
 */
public class CountMetricTest {
  @Test
  public void shouldGenerateDisplayData() {
    CountMetric<Integer> metric = new CountMetric<>("test", "test", MetricType.COUNT, 5, true);
    metric.add(2, 1000);
    metric.add(4, 2000);
    metric.add(6, 3000);
    metric.add(8, 4000);
    ArrayList<BucketData> buckets = metric.generateDisplayData(1500, TimeUnit.MILLISECONDS);
    assertThat(buckets.size()).isEqualTo(3);
    BucketData bd = buckets.get(2);
    assertThat(bd.getStartTimeMillis()).isEqualTo(2500l);
    assertThat(bd.getEndTimeMillis()).isEqualTo(4000l);
    assertThat(bd.getRisk()).isEqualTo(RiskLevel.HIGH);
    assertThat(bd.getStats().mean()).isEqualTo(7d);
    assertThat(bd.getDisplayValue()).isEqualTo("14");
  }
}
