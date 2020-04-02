package io.harness.perpetualtask.k8s.metrics.collector;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Duration;
import com.google.protobuf.util.Durations;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.grpc.utils.HTimestamps;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AggregatesTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldComputeMax() throws Exception {
    Aggregates aggregates = new Aggregates(Durations.fromSeconds(30));
    aggregates.update(1534, 123, "2019-11-26T07:00:32Z");
    aggregates.update(1342, 324, "2019-11-26T07:01:32Z");
    aggregates.update(1123, 350, "2019-11-26T07:02:32Z");
    assertThat(aggregates.getCpu().getMax()).isEqualTo(1534);
    assertThat(aggregates.getMemory().getMax()).isEqualTo(350);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldComputeAvg() throws Exception {
    Aggregates aggregates = new Aggregates(Durations.fromSeconds(30));
    aggregates.update(1534, 123, "2019-11-26T07:00:32Z");
    aggregates.update(1342, 324, "2019-11-26T07:01:32Z");
    aggregates.update(1123, 350, "2019-11-26T07:02:32Z");
    assertThat(aggregates.getCpu().getAverage()).isEqualTo(1333);
    assertThat(aggregates.getMemory().getAverage()).isEqualTo(265);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldProvideAggregateTimestamp() throws Exception {
    Aggregates aggregates = new Aggregates(Durations.fromSeconds(30));
    aggregates.update(1534, 123, "2019-11-26T07:00:32Z");
    aggregates.update(1342, 324, "2019-11-26T07:01:32Z");
    aggregates.update(1123, 350, "2019-11-26T07:02:32Z");
    assertThat(aggregates.getAggregateTimestamp()).isEqualTo(HTimestamps.parse("2019-11-26T07:00:32Z"));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldProvideAggregateWindow() throws Exception {
    Duration minWindow = Durations.fromSeconds(30);
    Aggregates aggregates = new Aggregates(minWindow);
    aggregates.update(1534, 123, "2019-11-26T07:00:32Z");
    aggregates.update(1342, 324, "2019-11-26T07:01:32Z");
    aggregates.update(1123, 350, "2019-11-26T07:02:32Z");
    assertThat(aggregates.getAggregateWindow()).isEqualTo(Durations.add(minWindow, Durations.fromMinutes(2)));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldHandleAdjacentDuplicate() throws Exception {
    Aggregates aggregates = new Aggregates(Durations.fromSeconds(30));
    aggregates.update(100, 2000, "2019-11-26T07:00:32Z");
    aggregates.update(100, 2000, "2019-11-26T07:00:32Z");
    aggregates.update(200, 3000, "2019-11-26T07:02:32Z");
    assertThat(aggregates.getCpu().getAverage()).isEqualTo(150);
    assertThat(aggregates.getMemory().getAverage()).isEqualTo(2500);
  }
}
