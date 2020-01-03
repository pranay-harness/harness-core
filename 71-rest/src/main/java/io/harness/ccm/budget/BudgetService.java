package io.harness.ccm.budget;

import io.harness.ccm.budget.entities.Budget;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetTableListData;

import java.sql.SQLException;
import java.util.List;

public interface BudgetService {
  String create(Budget budgetRecord);

  void update(String budgetId, Budget budget);

  Budget get(String budgetId);
  void incAlertCount(Budget budget, int threshold_index);

  List<Budget> list(String accountId);
  List<Budget> list(String accountId, Integer count, Integer startIndex);

  boolean delete(String budgetId);

  double getActualCost(Budget budget) throws SQLException;

  double getForecastCost(Budget budget);

  QLBudgetTableListData getBudgetData(Budget budget);
}
