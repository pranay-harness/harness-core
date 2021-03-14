package io.harness.ccm.billing;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._490_CE_COMMONS)
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
