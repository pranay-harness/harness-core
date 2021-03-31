package io.harness.ccm.billing.preaggregated;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CE)
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
