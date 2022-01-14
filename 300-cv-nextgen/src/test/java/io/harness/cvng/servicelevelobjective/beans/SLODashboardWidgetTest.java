package io.harness.cvng.servicelevelobjective.beans;

import static io.harness.cvng.CVNGTestConstants.TIME_FOR_TESTS;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.servicelevelobjective.beans.SLODashboardWidget.Point;
import io.harness.cvng.servicelevelobjective.beans.SLODashboardWidget.SLOGraphData;
import io.harness.rule.Owner;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SLODashboardWidgetTest extends CategoryTest {
  @Before
  public void setup() {}

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetBurnRate_withSinglePoint() {
    Instant now = TIME_FOR_TESTS;
    assertThat(SLOGraphData.builder()
                   .errorBudgetRemaining(98)
                   .errorBudgetRemainingPercentage(98)
                   .sloPerformanceTrend(Arrays.asList(Point.builder().timestamp(now.toEpochMilli()).value(10).build()))
                   .build()
                   .dailyBurnRate())
        .isCloseTo(2, offset(.001));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetBurnRate_withZeroPoint() {
    assertThat(SLOGraphData.builder()
                   .errorBudgetRemaining(98)
                   .errorBudgetRemainingPercentage(98)
                   .sloPerformanceTrend(Collections.emptyList())
                   .build()
                   .dailyBurnRate())
        .isCloseTo(0, offset(.001));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetBurnRate_multiplePoints() {
    Instant now = TIME_FOR_TESTS;
    assertThat(SLOGraphData.builder()
                   .errorBudgetRemaining(98)
                   .errorBudgetRemainingPercentage(98)
                   .sloPerformanceTrend(Arrays.asList(Point.builder().timestamp(now.toEpochMilli()).value(10).build(),
                       Point.builder().timestamp(now.plus(Duration.ofDays(2)).toEpochMilli()).value(10).build(),
                       Point.builder().timestamp(now.plus(Duration.ofDays(4)).toEpochMilli()).value(10).build()))
                   .build()
                   .dailyBurnRate())
        .isCloseTo(2 / 4.0, offset(.001));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetBurnRate_dayBoundary() {
    Instant now = Instant.parse("2020-07-27T00:00:00Z");
    assertThat(SLOGraphData.builder()
                   .errorBudgetRemaining(98)
                   .errorBudgetRemainingPercentage(98)
                   .sloPerformanceTrend(Arrays.asList(Point.builder().timestamp(now.toEpochMilli()).value(10).build(),
                       Point.builder().timestamp(now.plus(Duration.ofDays(2)).toEpochMilli()).value(10).build(),
                       Point.builder().timestamp(now.plus(Duration.ofDays(4)).toEpochMilli()).value(10).build()))
                   .build()
                   .dailyBurnRate())
        .isCloseTo(2 / 4.0, offset(.001));
  }
}