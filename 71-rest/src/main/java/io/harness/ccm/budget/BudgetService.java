package io.harness.ccm.budget;

import io.harness.ccm.budget.entities.Budget;

import java.util.List;

public interface BudgetService {
  String create(Budget budgetRecord);

  void update(String budgetId, Budget budget);

  Budget get(String budgetId);
  void incAlertCount(Budget budget, int threshold_index);

  List<Budget> list(String accountId);
  List<Budget> list(String accountId, Integer count, Integer startIndex);

  boolean delete(String budgetId);
}
