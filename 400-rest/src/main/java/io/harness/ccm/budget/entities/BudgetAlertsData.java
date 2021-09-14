/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.budget.entities;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.budget.AlertThresholdBase;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(HarnessModule._490_CE_COMMONS)
@OwnedBy(CE)
public class BudgetAlertsData {
  long time;
  String budgetId;
  String accountId;
  double alertThreshold;
  AlertThresholdBase alertBasedOn;
  double actualCost;
  double budgetedCost;
}
