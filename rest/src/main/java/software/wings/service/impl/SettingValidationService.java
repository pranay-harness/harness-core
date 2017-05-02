package software.wings.service.impl;

import software.wings.beans.AwsConfig;
import software.wings.beans.GcpConfig;
import software.wings.settings.SettingValue;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by anubhaw on 5/1/17.
 */
@Singleton
public class SettingValidationService {
  @Inject private AwsHelperService awsHelperService;
  @Inject private GcpHelperService gcpHelperService;

  public boolean validate(SettingValue settingValue) {
    if (settingValue instanceof GcpConfig) {
      gcpHelperService.validateCredential(((GcpConfig) settingValue).getServiceAccountKeyFileContent());
    } else if (settingValue instanceof AwsConfig) {
      awsHelperService.validateCredential(
          ((AwsConfig) settingValue).getAccessKey(), ((AwsConfig) settingValue).getSecretKey());
    }
    return true;
  }
}
