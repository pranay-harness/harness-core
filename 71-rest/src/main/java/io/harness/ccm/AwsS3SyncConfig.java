package io.harness.ccm;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AwsS3SyncConfig {
  String billingAccountId;
  String billingBucketRegion;
  String billingBucketPath;
}
