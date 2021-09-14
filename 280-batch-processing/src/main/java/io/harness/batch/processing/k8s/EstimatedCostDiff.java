/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.k8s;

import static java.math.BigDecimal.ROUND_DOWN;
import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import javax.annotation.Nonnull;
import lombok.Value;

@Value
public class EstimatedCostDiff {
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  @Nonnull BigDecimal oldCost;
  @Nonnull BigDecimal newCost;

  public BigDecimal getDiffAmount() {
    return newCost.subtract(oldCost);
  }

  public BigDecimal getDiffAmountPercent() {
    return ZERO.compareTo(oldCost) == 0
        ? null
        : getDiffAmount().multiply(BigDecimal.valueOf(100)).divide(oldCost, 2, ROUND_DOWN);
  }
}
