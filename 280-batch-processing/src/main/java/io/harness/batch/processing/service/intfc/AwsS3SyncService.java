package io.harness.batch.processing.service.intfc;

import io.harness.batch.processing.ccm.S3SyncRecord;

public interface AwsS3SyncService {
  boolean syncBuckets(S3SyncRecord s3SyncRecord);
}
