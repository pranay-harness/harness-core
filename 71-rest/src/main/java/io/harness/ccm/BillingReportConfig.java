package io.harness.ccm;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BillingReportConfig {
  String billingAccountId;
  String billingBucketRegion;
  String billingBucketPath;
  boolean isBillingReportEnabled;
}
