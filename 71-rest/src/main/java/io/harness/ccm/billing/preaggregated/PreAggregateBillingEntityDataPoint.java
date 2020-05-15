package io.harness.ccm.billing.preaggregated;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PreAggregateBillingEntityDataPoint {
  String id;
  String region;
  String awsLinkedAccount;
  String awsUsageType;
  String awsInstanceType;
  String awsService;
  double costTrend;
  double awsBlendedCost;
  double awsUnblendedCost;

  String gcpProjectId;
  String gcpProduct;
  String gcpSkuDescription;
  String gcpSkuId;
  double gcpTotalCost;
}
