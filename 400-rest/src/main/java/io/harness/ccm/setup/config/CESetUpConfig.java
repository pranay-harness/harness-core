/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.setup.config;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.secret.ConfigSecret;

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
  @ConfigSecret private String awsAccessKey;
  @ConfigSecret private String awsSecretKey;
  private String awsS3BucketName;
  @ConfigSecret private String masterAccountCloudFormationTemplateLink;
  @ConfigSecret private String linkedAccountCloudFormationTemplateLink;
  private String gcpProjectId;
  private String awsRoleName;
  //  QA: (SampleDataTestCE, AcG2IvPxQLmN-wWR8Ba3dg)
  //  Prod-free: (CE Sample Data, GOhbB8a7Q_q2QYn2iJSv-Q), (Harness-CS, jDOmhrFmSOGZJ1C91UC_hg)
  //  Prod-paid: (harness-demo, Sy3KVuK1SZy2Z7OLhbKlNg)
  private String sampleAccountId;
  @ConfigSecret private String azureAppClientId;
  @ConfigSecret private String azureAppClientSecret;
}
