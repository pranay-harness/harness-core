package io.harness.ccm.commons.entities.budget;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BudgetCostData {
  long time;
  double actualCost;
  double budgeted;
  double budgetVariance;
  double budgetVariancePercentage;
}
