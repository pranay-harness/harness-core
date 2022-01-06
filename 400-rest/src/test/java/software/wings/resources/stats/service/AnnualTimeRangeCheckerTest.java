/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources.stats.service;

import static io.harness.governance.TimeRangeOccurrence.ANNUAL;
import static io.harness.governance.TimeRangeOccurrence.DAILY;
import static io.harness.rule.OwnerRule.AGORODETKI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.resources.stats.model.TimeRange;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDC)
public class AnnualTimeRangeCheckerTest extends CategoryTest {
  private final TimeRangeChecker timeRangeChecker = ANNUAL.getTimeRangeChecker();

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfEndTimeForRepeatableRangeIsBeforeCurrentTime() {
    TimeRange timeRange =
        new TimeRange(1624785747000L, 1624792947000L, "Europe/Belgrade", false, null, 1624879347000L, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 16248789680000L)).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfCurrentMonthIsNotInGivenRangeWhenRangeCoversTwoYears() {
    TimeRange timeRange =
        new TimeRange(1638350547000L, 1646126547000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1648804947000L)).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfCurrentMonthIsNotInGivenRangeWhenRangeCoversSameYear() {
    TimeRange timeRange =
        new TimeRange(1609492947000L, 1621156947000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1654075347000L)).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfCurrentDayIsNotInGivenRangeWhenRangeCoversOneMonth() {
    TimeRange timeRange =
        new TimeRange(1609492947000L, 1610788947000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1643102547000L)).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnTrueIfCurrentDayIsInGivenRangeWhenRangeCoversOneMonth() {
    TimeRange timeRange =
        new TimeRange(1609492947000L, 1611220947000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1611220946999L)).isTrue();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfCurrentDayIsInGivenRangeWhenRangeCoversOneMonthButTimeIsBeforeStartTime() {
    TimeRange timeRange =
        new TimeRange(1609492947000L, 1611220947000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1609492946999L)).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfCurrentDayIsInGivenRangeWhenRangeCoversOneMonthButTimeIsAfterEndTime() {
    TimeRange timeRange =
        new TimeRange(1609492947000L, 1611220947000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1611220947001L)).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnTrueIfCurrentMonthIsInGivenRangeWhenRangeCoversMultipleMonths() {
    TimeRange timeRange =
        new TimeRange(1609492947000L, 1621156947000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1646126547000L)).isTrue();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfCurrentMonthIsEqualsToStartMonthButDayIsBeforeStartDay() {
    TimeRange timeRange =
        new TimeRange(1609838547000L, 1610702547000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1609752147000L)).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnTrueIfCurrentMonthIsEqualsToStartMonthAndDayIsAfterStartDay() {
    TimeRange timeRange =
        new TimeRange(1609838547000L, 1610702547000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1609924947000L)).isTrue();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnTrueIfCurrentMonthAndDayIsEqualsToStartMonthAndDayAndTimeIsAfterStartTime() {
    TimeRange timeRange =
        new TimeRange(1609838547000L, 1612689747000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1609838547001L)).isTrue();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfCurrentMonthAndDayIsEqualsToStartMonthAndDayButTimeIsBeforeStartTime() {
    TimeRange timeRange =
        new TimeRange(1609838547000L, 1612689747000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1609838546999L)).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfCurrentMonthIsEqualsToEndMonthButDayIsAfterEndDay() {
    TimeRange timeRange =
        new TimeRange(1609838547000L, 1612689747000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1644312147000L)).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnTrueIfCurrentMonthIsEqualsToEndMonthAndDayIsBeforeEndDay() {
    TimeRange timeRange =
        new TimeRange(1609838547000L, 1612689747000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1644052947000L)).isTrue();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnTrueIfCurrentMonthAndDayIsEqualsToEndMonthAndDayAndTimeIsBeforeEndTime() {
    TimeRange timeRange =
        new TimeRange(1609838547000L, 1612689747000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1644222147000L)).isTrue();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfCurrentMonthAndDayIsEqualsToEndMonthAndDayButTimeIsAfterEndTime() {
    TimeRange timeRange =
        new TimeRange(1609838547000L, 1612689747000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1644229347000L)).isFalse();
  }
}
