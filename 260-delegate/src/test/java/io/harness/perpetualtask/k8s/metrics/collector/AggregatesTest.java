/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.k8s.metrics.collector;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.grpc.utils.HTimestamps;
import io.harness.rule.Owner;

import com.google.protobuf.Duration;
import com.google.protobuf.util.Durations;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AggregatesTest extends CategoryTest {
  private static final String TIMESTAMP_1 = "2019-11-26T07:00:32Z";
  private static final String TIMESTAMP_2 = "2019-11-26T07:01:32Z";
  private static final String TIMESTAMP_3 = "2019-11-26T07:02:32Z";

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldComputeMax() throws Exception {
    Aggregates aggregates = new Aggregates(Durations.fromSeconds(30));
    aggregates.update(1534, 123, TIMESTAMP_1);
    aggregates.update(1342, 324, TIMESTAMP_2);
    aggregates.update(1123, 350, TIMESTAMP_3);
    assertThat(aggregates.getCpu().getMax()).isEqualTo(1534);
    assertThat(aggregates.getMemory().getMax()).isEqualTo(350);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldComputeAvg() throws Exception {
    Aggregates aggregates = new Aggregates(Durations.fromSeconds(30));
    aggregates.update(1534, 123, TIMESTAMP_1);
    aggregates.update(1342, 324, TIMESTAMP_2);
    aggregates.update(1123, 350, TIMESTAMP_3);
    assertThat(aggregates.getCpu().getAverage()).isEqualTo(1333);
    assertThat(aggregates.getMemory().getAverage()).isEqualTo(265);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldComputeAvgStorage() throws Exception {
    Aggregates aggregates = new Aggregates(Durations.fromSeconds(30));
    aggregates.updateStorage(100, 10, TIMESTAMP_1);
    aggregates.updateStorage(300, 30, TIMESTAMP_2);
    aggregates.updateStorage(200, 110, TIMESTAMP_3);

    assertThat(aggregates.getStorageCapacity().getAverage()).isEqualTo(200);
    assertThat(aggregates.getStorageUsed().getAverage()).isEqualTo(50);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldUpdateStorageAggregateWindow() throws Exception {
    Aggregates aggregates = new Aggregates(Durations.fromSeconds(30));
    aggregates.updateStorage(100, 10, TIMESTAMP_1);

    assertThat(aggregates.getAggregateWindow()).isEqualTo(Durations.fromSeconds(30));
    assertThat(aggregates.getAggregateTimestamp()).isEqualTo(HTimestamps.parse(TIMESTAMP_1));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldProvideAggregateTimestamp() throws Exception {
    Aggregates aggregates = new Aggregates(Durations.fromSeconds(30));
    aggregates.update(1534, 123, TIMESTAMP_1);
    aggregates.update(1342, 324, TIMESTAMP_2);
    aggregates.update(1123, 350, TIMESTAMP_3);
    assertThat(aggregates.getAggregateTimestamp()).isEqualTo(HTimestamps.parse(TIMESTAMP_1));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldProvideAggregateWindow() throws Exception {
    Duration minWindow = Durations.fromSeconds(30);
    Aggregates aggregates = new Aggregates(minWindow);
    aggregates.update(1534, 123, TIMESTAMP_1);
    aggregates.update(1342, 324, TIMESTAMP_2);
    aggregates.update(1123, 350, TIMESTAMP_3);
    assertThat(aggregates.getAggregateWindow()).isEqualTo(Durations.add(minWindow, Durations.fromMinutes(2)));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldHandleAdjacentDuplicate() throws Exception {
    Aggregates aggregates = new Aggregates(Durations.fromSeconds(30));
    aggregates.update(100, 2000, TIMESTAMP_1);
    aggregates.update(100, 2000, TIMESTAMP_1);
    aggregates.update(200, 3000, TIMESTAMP_3);
    assertThat(aggregates.getCpu().getAverage()).isEqualTo(150);
    assertThat(aggregates.getMemory().getAverage()).isEqualTo(2500);
  }
}
