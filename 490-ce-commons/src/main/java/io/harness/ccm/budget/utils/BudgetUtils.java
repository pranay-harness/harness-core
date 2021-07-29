package io.harness.ccm.budget.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.budget.BudgetScope;
import io.harness.ccm.commons.entities.billing.Budget;
import io.harness.exception.InvalidRequestException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CE)
public class BudgetUtils {
  private static final double BUDGET_AMOUNT_UPPER_LIMIT = 100000000;
  private static final String NO_BUDGET_AMOUNT_EXCEPTION = "Error in creating budget. No budget amount specified.";
  private static final String BUDGET_AMOUNT_NOT_WITHIN_BOUNDS_EXCEPTION =
      "Error in creating budget. The budget amount should be positive and less than 100 million dollars.";
  private static final String BUDGET_NAME_EXISTS_EXCEPTION =
      "Error in creating budget. Budget with given name already exists";
  private static final String BUDGET_NAME_NOT_PROVIDED_EXCEPTION = "Please provide a name for clone budget.";
  public static final String INVALID_ENTITY_ID_EXCEPTION =
      "Error in create/update budget operation. Some of the appliesTo ids are invalid.";
  public static final String INVALID_PERSPECTIVE_ID_EXCEPTION = "Invalid perspective id";
  public static final String INVALID_BUDGET_ID_EXCEPTION = "Invalid budget id";
  private static final String UNDEFINED_BUDGET = "undefined";
  private static final String DEFAULT_TIMEZONE = "GMT";
  public static final long ONE_DAY_MILLIS = 86400000;

  public static void validateBudget(Budget budget, List<Budget> existingBudgets) {
    validateBudgetAmount(budget);
    validateBudgetName(budget, existingBudgets);
  }

  private static void validateBudgetAmount(Budget budget) {
    if (budget.getBudgetAmount() == null) {
      throw new InvalidRequestException(NO_BUDGET_AMOUNT_EXCEPTION);
    }
    if (budget.getBudgetAmount() < 0 || budget.getBudgetAmount() > BUDGET_AMOUNT_UPPER_LIMIT) {
      throw new InvalidRequestException(BUDGET_AMOUNT_NOT_WITHIN_BOUNDS_EXCEPTION);
    }
  }

  private static void validateBudgetName(Budget budget, List<Budget> existingBudgets) {
    if (!existingBudgets.isEmpty() && (!existingBudgets.get(0).getUuid().equals(budget.getUuid()))) {
      throw new InvalidRequestException(BUDGET_NAME_EXISTS_EXCEPTION);
    }
  }

  public static void validateCloneBudgetName(String cloneBudgetName) {
    if (cloneBudgetName.equals(UNDEFINED_BUDGET)) {
      throw new InvalidRequestException(BUDGET_NAME_NOT_PROVIDED_EXCEPTION);
    }
  }

  public static String[] getAppliesToIds(BudgetScope scope) {
    String[] entityIds = {};
    if (scope == null) {
      return entityIds;
    }
    log.debug(
        "Budget scope info is: {} {} {}", scope.getBudgetScopeType(), scope.getEntityIds(), scope.getEntityNames());
    return scope.getEntityIds().toArray(new String[0]);
  }

  public static long getStartOfMonthForCurrentBillingCycle() {
    ZoneId zoneId = ZoneId.of(DEFAULT_TIMEZONE);
    LocalDate today = LocalDate.now(zoneId);
    ZonedDateTime zdtStart = today.withDayOfMonth(1).atStartOfDay(zoneId);
    return zdtStart.toEpochSecond() * 1000;
  }

  public static long getEndOfMonthForCurrentBillingCycle() {
    ZoneId zoneId = ZoneId.of(DEFAULT_TIMEZONE);
    LocalDate today = LocalDate.now(zoneId);
    Calendar cal = Calendar.getInstance();
    int daysInMonth = cal.getActualMaximum(Calendar.DATE);
    ZonedDateTime zdtStart = today.withDayOfMonth(daysInMonth).atStartOfDay(zoneId);
    return zdtStart.toEpochSecond() * 1000 + ONE_DAY_MILLIS - 1000;
  }

  public static long getStartOfMonth(boolean prevMonth) {
    Calendar c = Calendar.getInstance();
    c.setTimeZone(TimeZone.getTimeZone(DEFAULT_TIMEZONE));
    c.set(Calendar.DAY_OF_MONTH, 1);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    if (prevMonth) {
      c.add(Calendar.MONTH, -1);
    }
    return c.getTimeInMillis();
  }

  public static long getStartOfCurrentDay() {
    ZoneId zoneId = ZoneId.of(DEFAULT_TIMEZONE);
    LocalDate today = LocalDate.now(zoneId);
    ZonedDateTime zdtStart = today.atStartOfDay(zoneId);
    return zdtStart.toEpochSecond() * 1000;
  }

  public static long getStartTimeForForecasting() {
    return getStartOfCurrentDay() - 30 * ONE_DAY_MILLIS;
  }

  public static long getStartTimeForCurrentBillingCycle() {
    Calendar c = Calendar.getInstance();
    c.set(Calendar.DAY_OF_MONTH, 1);
    c.set(Calendar.YEAR, c.get(Calendar.YEAR) - 1);
    return c.getTimeInMillis();
  }

  public static double getBudgetVariance(double budgetedAmount, double actualCost) {
    return actualCost - budgetedAmount;
  }

  public static double getBudgetVariancePercentage(double budgetVariance, double budgetedAmount) {
    return budgetedAmount != 0 ? (budgetVariance / budgetedAmount) * 100 : 0.0;
  }
}
