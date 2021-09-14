/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
