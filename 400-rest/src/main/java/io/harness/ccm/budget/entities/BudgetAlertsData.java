package io.harness.ccm.budget.entities;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.budget.AlertThresholdBase;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(Module._490_CE_COMMONS)
public class BudgetAlertsData {
  long time;
  String budgetId;
  String accountId;
  double alertThreshold;
  AlertThresholdBase alertBasedOn;
  double actualCost;
  double budgetedCost;
}
