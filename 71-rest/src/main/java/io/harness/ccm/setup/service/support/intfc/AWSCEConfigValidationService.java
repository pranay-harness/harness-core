package io.harness.ccm.setup.service.support.intfc;

import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAwsConfig;

public interface AWSCEConfigValidationService {
  void verifyCrossAccountAttributes(SettingAttribute settingAttribute);
  String validateCURReportAccessAndReturnS3Region(CEAwsConfig awsConfig);
}
