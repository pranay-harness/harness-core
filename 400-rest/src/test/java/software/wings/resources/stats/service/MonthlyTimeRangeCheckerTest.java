/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources.stats.service;

import static io.harness.governance.TimeRangeOccurrence.DAILY;
import static io.harness.governance.TimeRangeOccurrence.MONTHLY;
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
public class MonthlyTimeRangeCheckerTest extends CategoryTest {
  private final TimeRangeChecker timeRangeChecker = MONTHLY.getTimeRangeChecker();

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
  public void shouldReturnFalseIfCurrentDayDoesNotMatchGivenRangeWhenRangeCoversTwoMonth() {
    TimeRange timeRange =
        new TimeRange(1624785747000L, 1625151338000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1627311338000L)).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfCurrentDayDoesNotMatchGivenRangeWhenRangeCoversSameMonth() {
    TimeRange timeRange =
        new TimeRange(1624785747000L, 1625044947000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1625151338000L)).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnTrueIfCurrentDayMatchesGivenRangeAndTimeWhenRangeIsForSingleDay() {
    TimeRange timeRange =
        new TimeRange(1624785747000L, 1624800147000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1630063538000L)).isTrue();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfCurrentDayMatchesGivenRangeWhenRangeIsForSingleDayButBeforeStartTime() {
    TimeRange timeRange =
        new TimeRange(1624785747000L, 1624800147000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1624785746999L)).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfCurrentDayMatchesGivenRangeWhenRangeIsForSingleDayButAfterEndTime() {
    TimeRange timeRange =
        new TimeRange(1624785747000L, 1624800147000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1624800147001L)).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfCurrentDayMatchesStartDayButIsBeforeStartTimeOfGivenRangeWhenRangeIsForMultipleDays() {
    TimeRange timeRange =
        new TimeRange(1624785747000L, 1625217747000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1627377746999L)).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void
  shouldReturnTrueIfCurrentDayMatchesStartDayAndTimeIsAfterStartTimeOfGivenRangeWhenRangeIsForMultipleDays() {
    TimeRange timeRange =
        new TimeRange(1624785747000L, 1625217747000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1624785747001L)).isTrue();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfCurrentDayMatchesEndDayButIsAfterEndTimeOfGivenRangeWhenRangeIsForMultipleDays() {
    TimeRange timeRange =
        new TimeRange(1624785747000L, 1625217747000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1625217747001L)).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnTrueIfCurrentDayMatchesEndDayAndIsBeforeEndTimeOfGivenRangeWhenRangeIsForMultipleDays() {
    TimeRange timeRange =
        new TimeRange(1624785747000L, 1625217747000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1625217746999L)).isTrue();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnTrueIfCurrentDayIsBetweenGivenRangeForMultipleDays() {
    TimeRange timeRange =
        new TimeRange(1624785747000L, 1625217747000L, "Europe/Belgrade", false, null, Long.MAX_VALUE, DAILY, false);
    assertThat(timeRangeChecker.istTimeInRange(timeRange, 1638177747000L)).isTrue();
  }
}
