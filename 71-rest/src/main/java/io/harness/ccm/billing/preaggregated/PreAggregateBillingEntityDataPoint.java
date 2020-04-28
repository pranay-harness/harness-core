package io.harness.ccm.billing.preaggregated;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PreAggregateBillingEntityDataPoint {
  String id;
  String awsRegion;
  String awsLinkedAccount;
  String awsUsageType;
  String awsInstanceType;
  String awsService;
  double costTrend;
  double awsBlendedCost;
  double awsUnblendedCost;
}
