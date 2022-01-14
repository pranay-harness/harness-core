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
