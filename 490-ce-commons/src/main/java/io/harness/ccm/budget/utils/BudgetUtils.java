package io.harness.ccm.budget.utils;

import io.harness.ccm.budget.BudgetScope;
import io.harness.ccm.commons.entities.billing.Budget;
import io.harness.exception.InvalidRequestException;

import java.util.List;

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
  private static final String UNDEFINED_BUDGET = "undefined";

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
    return scope.getEntityIds().toArray(new String[0]);
  }
}
