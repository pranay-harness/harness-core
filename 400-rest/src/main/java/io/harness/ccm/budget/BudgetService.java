package io.harness.ccm.budget;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.budget.QLBudgetDataList;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetTableData;

import java.util.List;

@TargetModule(Module._375_CE_GRAPHQL)
public interface BudgetService {
  String create(Budget budgetRecord);
  String clone(String budgetId, String budgetName, String accountId);

  Budget get(String budgetId, String accountId);

  void update(String budgetId, Budget budget);
  void setThresholdCrossedTimestamp(Budget budget, int thresholdIndex, long crossedAt);
  void incAlertCount(Budget budget, int thresholdIndex);

  List<Budget> list(String accountId);
  List<Budget> list(String accountId, Integer count, Integer startIndex);
  List<Budget> list(String accountId, String viewId);
  int getBudgetCount(String accountId);

  boolean delete(String budgetId, String accountId);

  double getActualCost(Budget budget);

  double getForecastCost(Budget budget);

  QLBudgetDataList getBudgetData(Budget budget);

  QLBudgetTableData getBudgetDetails(Budget budget);

  boolean isStartOfMonth();

  boolean isAlertSentInCurrentMonth(long crossedAt);
}
