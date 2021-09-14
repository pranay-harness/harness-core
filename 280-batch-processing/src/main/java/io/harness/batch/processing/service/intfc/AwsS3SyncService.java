/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.service.intfc;

import io.harness.batch.processing.ccm.S3SyncRecord;

public interface AwsS3SyncService {
  boolean syncBuckets(S3SyncRecord s3SyncRecord);
}
