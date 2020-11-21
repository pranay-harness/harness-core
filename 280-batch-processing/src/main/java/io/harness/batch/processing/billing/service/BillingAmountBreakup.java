package io.harness.batch.processing.billing.service;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BillingAmountBreakup {
  private BigDecimal billingAmount;
  private BigDecimal cpuBillingAmount;
  private BigDecimal memoryBillingAmount;
}
