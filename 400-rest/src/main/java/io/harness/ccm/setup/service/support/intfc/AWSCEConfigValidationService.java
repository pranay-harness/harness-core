/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.setup.service.support.intfc;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.AwsS3BucketDetails;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAwsConfig;

@OwnedBy(CE)
public interface AWSCEConfigValidationService {
  void verifyCrossAccountAttributes(SettingAttribute settingAttribute);
  AwsS3BucketDetails validateCURReportAccessAndReturnS3Config(CEAwsConfig awsConfig);
  boolean updateBucketPolicy(CEAwsConfig awsConfig);
}
