package io.harness.batch.processing.billing.service;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class BillingAmountBreakup {
  private BigDecimal billingAmount;
  private BigDecimal cpuBillingAmount;
  private BigDecimal memoryBillingAmount;
}