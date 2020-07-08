package io.harness.ccm.setup.service.support.intfc;

import software.wings.beans.AwsS3BucketDetails;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAwsConfig;

public interface AWSCEConfigValidationService {
  void verifyCrossAccountAttributes(SettingAttribute settingAttribute);
  AwsS3BucketDetails validateCURReportAccessAndReturnS3Config(CEAwsConfig awsConfig);
  boolean updateBucketPolicy(CEAwsConfig awsConfig);
}
