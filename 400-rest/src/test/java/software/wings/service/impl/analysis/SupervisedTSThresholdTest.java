/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.analysis;

import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.metrics.MetricType;
import software.wings.metrics.Threshold;
import software.wings.metrics.ThresholdComparisonType;
import software.wings.metrics.ThresholdType;

import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SupervisedTSThresholdTest extends WingsBaseTest {
  private SupervisedTSThreshold supervisedTSThreshold =
      SupervisedTSThreshold.builder().maxThreshold(2.1).minThreshold(-1.8).build();

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void getThresholdsTestInfraMetric() {
    supervisedTSThreshold.setMetricType(MetricType.INFRA);
    List<Threshold> thresholds = SupervisedTSThreshold.getThresholds(supervisedTSThreshold);

    assertThat(thresholds.size()).isEqualTo(1);
    assertThat(thresholds.get(0).getMl()).isEqualTo(1.8);
    assertThat(ThresholdType.ALERT_HIGHER_OR_LOWER).isEqualByComparingTo(thresholds.get(0).getThresholdType());
    assertThat(ThresholdComparisonType.DELTA).isEqualByComparingTo(thresholds.get(0).getComparisonType());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void getThresholdsTestResponseTimeMetric() {
    supervisedTSThreshold.setMetricType(MetricType.RESP_TIME);
    List<Threshold> thresholds = SupervisedTSThreshold.getThresholds(supervisedTSThreshold);

    assertThat(thresholds.size()).isEqualTo(1);
    assertThat(thresholds.get(0).getMl()).isEqualTo(1.8);
    assertThat(ThresholdType.ALERT_WHEN_HIGHER).isEqualByComparingTo(thresholds.get(0).getThresholdType());
    assertThat(ThresholdComparisonType.DELTA).isEqualByComparingTo(thresholds.get(0).getComparisonType());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void getThresholdsTestThroughputMetric() {
    supervisedTSThreshold.setMetricType(MetricType.THROUGHPUT);
    List<Threshold> thresholds = SupervisedTSThreshold.getThresholds(supervisedTSThreshold);

    assertThat(thresholds.size()).isEqualTo(1);
    assertThat(thresholds.get(0).getMl()).isEqualTo(2.1);
    assertThat(ThresholdType.ALERT_WHEN_LOWER).isEqualByComparingTo(thresholds.get(0).getThresholdType());
    assertThat(ThresholdComparisonType.DELTA).isEqualByComparingTo(thresholds.get(0).getComparisonType());
  }
}
