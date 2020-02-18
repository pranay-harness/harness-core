package io.harness.batch.processing.ccm;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class S3SyncRecord {
  String accountId;
  String settingId;
  String billingAccountId;
  String billingBucketRegion;
  String billingBucketPath;
  String roleArn;
  String externalId;
}
