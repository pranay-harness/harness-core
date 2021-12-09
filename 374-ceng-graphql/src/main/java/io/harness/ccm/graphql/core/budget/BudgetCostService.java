package io.harness.ccm.graphql.core.budget;

import io.harness.ccm.commons.entities.billing.Budget;
import io.harness.ccm.commons.entities.budget.BudgetData;

public interface BudgetCostService {
  double getActualCost(Budget budget);
  double getForecastCost(Budget budget);
  double getLastPeriodCost(Budget budget);
  BudgetData getBudgetTimeSeriesStats(Budget budget);
}
