package io.harness.ccm;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BillingReportConfig {
  String billingAccountId;
  String billingBucketRegion;
  String billingBucketPath;
  String roleArn;
  String externalId;
  boolean isBillingReportEnabled;
}
