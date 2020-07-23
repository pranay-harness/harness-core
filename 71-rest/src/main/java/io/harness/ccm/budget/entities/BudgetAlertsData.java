package io.harness.ccm.budget.entities;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BudgetAlertsData {
  long time;
  String budgetId;
  String accountId;
  double alertThreshold;
  double actualCost;
  double budgetedCost;
}
