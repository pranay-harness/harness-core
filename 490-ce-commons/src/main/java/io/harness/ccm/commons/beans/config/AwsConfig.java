/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.commons.beans.config;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CE)
public class AwsConfig {
  private String accessKey;
  private String secretKey;
  private String destinationBucket;
  private String destinationBucketsCount;
  private String harnessAwsAccountId;
  private String awsConnectorTemplate;
}
