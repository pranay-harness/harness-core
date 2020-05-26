package io.harness.batch.processing.k8s;

import static java.math.BigDecimal.ROUND_DOWN;
import static java.math.BigDecimal.ZERO;

import lombok.Value;

import java.math.BigDecimal;
import javax.annotation.Nonnull;

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
