package io.harness.ccm.budget;

import io.harness.ccm.budget.entities.Budget;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetTableListData;

import java.util.List;

public interface BudgetService {
  String create(Budget budgetRecord);

  Budget get(String budgetId);

  void update(String budgetId, Budget budget);
  void setThresholdCrossedTimestamp(Budget budget, int thresholdIndex, long crossedAt);
  void incAlertCount(Budget budget, int thresholdIndex);

  List<Budget> list(String accountId);
  List<Budget> list(String accountId, Integer count, Integer startIndex);

  boolean delete(String budgetId);

  double getActualCost(Budget budget);

  double getForecastCost(Budget budget);

  QLBudgetTableListData getBudgetData(Budget budget);
}
