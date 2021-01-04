package io.harness.ccm.billing;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GcpBillingEntityDataPoints {
  String id;
  String name;
  String projectNumber;
  String productType;
  String usage;
  String region;
  double totalCost;
  double discounts;
  double subTotal;
}
