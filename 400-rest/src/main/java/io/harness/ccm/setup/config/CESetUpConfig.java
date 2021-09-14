/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.setup.config;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Singleton
@OwnedBy(CE)
public class CESetUpConfig {
  private String awsAccountId;
  private String awsAccessKey;
  private String awsSecretKey;
  private String awsS3BucketName;
  private String masterAccountCloudFormationTemplateLink;
  private String linkedAccountCloudFormationTemplateLink;
  private String gcpProjectId;
  private String awsRoleName;
  //  QA: (SampleDataTestCE, AcG2IvPxQLmN-wWR8Ba3dg)
  //  Prod-free: (CE Sample Data, GOhbB8a7Q_q2QYn2iJSv-Q), (Harness-CS, jDOmhrFmSOGZJ1C91UC_hg)
  //  Prod-paid: (harness-demo, Sy3KVuK1SZy2Z7OLhbKlNg)
  private String sampleAccountId;
  private String azureAppClientId;
  private String azureAppClientSecret;
}
