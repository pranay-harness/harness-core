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
  double awsBlendedCost;
  double awsUnblendedCost;
}
