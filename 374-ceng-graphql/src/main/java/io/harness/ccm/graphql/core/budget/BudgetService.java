package io.harness.ccm.graphql.core.budget;

import io.harness.ccm.commons.entities.billing.Budget;
import io.harness.ccm.commons.entities.budget.BudgetData;

import java.util.List;

public interface BudgetService {
  String create(Budget budgetRecord);
  String clone(String budgetId, String budgetName, String accountId);

  Budget get(String budgetId, String accountId);

  void update(String budgetId, Budget budget);

  List<Budget> list(String accountId);
  List<Budget> list(String accountId, String viewId);

  boolean delete(String budgetId, String accountId);

  BudgetData getBudgetTimeSeriesStats(Budget budget);
}
