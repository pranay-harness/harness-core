/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources.stats.service;

import static io.harness.governance.TimeRangeOccurrence.DAILY;
import static io.harness.governance.TimeRangeOccurrence.WEEKLY;
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
public class WeeklyTimeRangeCheckerTest extends CategoryTest {
  private final TimeRangeChecker timeRangeChecker = WEEKLY.getTimeRangeChecker();

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
  public void shouldReturnFalseIfCurrentDayOfWeekIsNotInGivenRepeatableRange() {
    TimeRange timeRange =
        new TimeRange(1624785747000L, 1624792947000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, WEEKLY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 16248789680000L)).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfCurrentDayOfWeekIsInGivenRepeatableRangeButAfterHoursForSingleDayRange() {
    TimeRange timeRange =
        new TimeRange(1624785747000L, 1624792947000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, WEEKLY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1624792947001L)).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfCurrentDayOfWeekIsInGivenRepeatableRangeButBeforeHoursForSingleDayRange() {
    TimeRange timeRange =
        new TimeRange(1624785747000L, 1624792947000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, WEEKLY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1624785746999L)).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnTrueIfCurrentDayOfWeekIsInGivenRepeatableRangeAndTimeForSingleDayRange() {
    TimeRange timeRange =
        new TimeRange(1624785747000L, 1624792947000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, WEEKLY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1624792887000L)).isTrue();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfCurrentDayOfWeekIsInGivenRepeatableRangeButBeforeHoursForMultipleDaysRange() {
    TimeRange timeRange =
        new TimeRange(1624785747000L, 1625063083000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, WEEKLY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1625390546999L)).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfCurrentDayOfWeekIsInGivenRepeatableRangeButAfterHoursForMultipleDaysRange() {
    TimeRange timeRange =
        new TimeRange(1624785747000L, 1625063083000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, WEEKLY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1625063687801L)).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnTrueIfCurrentDayOfWeekIsFirstDayOfGivenRepeatableRangeAndTimeIsAfterStartTime() {
    TimeRange timeRange =
        new TimeRange(1624785747000L, 1624868749000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, WEEKLY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1624785747001L)).isTrue();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnTrueIfCurrentDayOfWeekIsLastDayOfGivenRepeatableRangeAndTimeIsBeforeTime() {
    TimeRange timeRange =
        new TimeRange(1624785747000L, 1624868749000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, WEEKLY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1624868748999L)).isTrue();
  }
}
