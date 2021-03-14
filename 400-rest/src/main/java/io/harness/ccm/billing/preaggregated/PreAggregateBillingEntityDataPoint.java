package io.harness.ccm.billing.preaggregated;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._490_CE_COMMONS)
public class PreAggregateBillingEntityDataPoint {
  String id;
  String region;
  String awsLinkedAccount;
  String awsUsageType;
  String awsInstanceType;
  String awsService;
  String awsTag;
  double costTrend;
  double awsBlendedCost;
  double awsUnblendedCost;

  String gcpProjectId;
  String gcpProduct;
  String gcpSkuDescription;
  String gcpSkuId;
  String gcpLabel;
  double gcpDiscount;
  double gcpTotalCost;
  double gcpSubTotalCost;
}
