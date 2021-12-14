package io.harness.cvng.servicelevelobjective.entities;

import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.servicelevelobjective.beans.DayOfWeek;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelObjective.MonthlyCalenderTarget;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelObjective.RollingSLOTarget;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelObjective.WeeklyCalenderTarget;
import io.harness.rule.Owner;

import java.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ServiceLevelObjectiveTest extends CategoryTest {
  private BuilderFactory builderFactory;
  @Before
  public void setup() {
    builderFactory = BuilderFactory.getDefault();
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetCurrentTimeRange_rolling() {
    ServiceLevelObjective serviceLevelObjective =
        builderFactory.getServiceLevelObjectiveBuilder()
            .sloTarget(RollingSLOTarget.builder().periodLengthDays(30).build())
            .build();
    assertThat(serviceLevelObjective.getSloTarget().getCurrentTimeRange(LocalDate.parse("2021-12-03")))
        .isEqualTo(ServiceLevelObjective.TimePeriod.builder()
                       .startDate(LocalDate.parse("2021-11-03"))
                       .endDate(LocalDate.parse("2021-12-03"))
                       .build());
    assertThat(serviceLevelObjective.getSloTarget().getCurrentTimeRange(LocalDate.parse("2021-12-31")))
        .isEqualTo(ServiceLevelObjective.TimePeriod.builder()
                       .startDate(LocalDate.parse("2021-12-01"))
                       .endDate(LocalDate.parse("2021-12-31"))
                       .build());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetCurrentTimeRange_calenderWeekly() {
    ServiceLevelObjective serviceLevelObjective =
        builderFactory.getServiceLevelObjectiveBuilder()
            .sloTarget(WeeklyCalenderTarget.builder().dayOfWeek(DayOfWeek.MONDAY).build())
            .build();
    assertThat(serviceLevelObjective.getSloTarget().getCurrentTimeRange(LocalDate.parse("2021-12-03")))
        .isEqualTo(ServiceLevelObjective.TimePeriod.builder()
                       .startDate(LocalDate.parse("2021-11-30"))
                       .endDate(LocalDate.parse("2021-12-06"))
                       .build());
    assertThat(serviceLevelObjective.getSloTarget().getCurrentTimeRange(LocalDate.parse("2021-12-06")))
        .isEqualTo(ServiceLevelObjective.TimePeriod.builder()
                       .startDate(LocalDate.parse("2021-11-30"))
                       .endDate(LocalDate.parse("2021-12-06"))
                       .build());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetCurrentTimeRange_calenderQuarterly() {
    ServiceLevelObjective serviceLevelObjective =
        builderFactory.getServiceLevelObjectiveBuilder()
            .sloTarget(ServiceLevelObjective.QuarterlyCalenderTarget.builder().build())
            .build();
    assertThat(serviceLevelObjective.getSloTarget().getCurrentTimeRange(LocalDate.parse("2021-12-03")))
        .isEqualTo(ServiceLevelObjective.TimePeriod.builder()
                       .startDate(LocalDate.parse("2021-10-01"))
                       .endDate(LocalDate.parse("2021-12-31"))
                       .build());
    assertThat(serviceLevelObjective.getSloTarget().getCurrentTimeRange(LocalDate.parse("2021-12-31")))
        .isEqualTo(ServiceLevelObjective.TimePeriod.builder()
                       .startDate(LocalDate.parse("2021-10-01"))
                       .endDate(LocalDate.parse("2021-12-31"))
                       .build());
    assertThat(serviceLevelObjective.getSloTarget().getCurrentTimeRange(LocalDate.parse("2021-10-01")))
        .isEqualTo(ServiceLevelObjective.TimePeriod.builder()
                       .startDate(LocalDate.parse("2021-10-01"))
                       .endDate(LocalDate.parse("2021-12-31"))
                       .build());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetCurrentTimeRange_calenderMonthlyMiddleRange() {
    testCurrentTimeRangeMonthly(3, "2021-12-03", "2021-11-04", "2021-12-03");
    testCurrentTimeRangeMonthly(3, "2021-11-30", "2021-11-04", "2021-12-03");
    testCurrentTimeRangeMonthly(3, "2021-11-04", "2021-11-04", "2021-12-03");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetCurrentTimeRange_calenderMonthlyEndOfMonth() {
    testCurrentTimeRangeMonthly(31, "2021-12-03", "2021-12-01", "2021-12-31");
    testCurrentTimeRangeMonthly(31, "2021-11-30", "2021-11-01", "2021-11-30");
    testCurrentTimeRangeMonthly(31, "2021-02-28", "2021-02-01", "2021-02-28");
    testCurrentTimeRangeMonthly(28, "2021-12-03", "2021-11-29", "2021-12-28");
    testCurrentTimeRangeMonthly(28, "2021-11-30", "2021-11-29", "2021-12-28");
    testCurrentTimeRangeMonthly(28, "2021-11-28", "2021-10-29", "2021-11-28");
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetTotalErrorBudgetMinutes() {
    ServiceLevelObjective serviceLevelObjective =
        builderFactory.getServiceLevelObjectiveBuilder()
            .sloTargetPercentage(99.0)
            .sloTarget(WeeklyCalenderTarget.builder().dayOfWeek(DayOfWeek.MONDAY).build())
            .build();
    assertThat(serviceLevelObjective.getTotalErrorBudgetMinutes(LocalDate.parse("2021-12-03"))).isEqualTo(101);
  }

  private void testCurrentTimeRangeMonthly(
      int windowEndDate, String currentDate, String expectedStartDate, String expectedEndDate) {
    ServiceLevelObjective serviceLevelObjective =
        builderFactory.getServiceLevelObjectiveBuilder()
            .sloTarget(MonthlyCalenderTarget.builder().windowEndDayOfMonth(windowEndDate).build())
            .build();
    assertThat(serviceLevelObjective.getSloTarget().getCurrentTimeRange(LocalDate.parse(currentDate)))
        .isEqualTo(ServiceLevelObjective.TimePeriod.builder()
                       .startDate(LocalDate.parse(expectedStartDate))
                       .endDate(LocalDate.parse(expectedEndDate))
                       .build());
  }
}