package software.wings.service.impl;

import software.wings.beans.AwsConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.Base;
import software.wings.beans.DockerConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.config.NexusConfig;
import software.wings.service.intfc.BuildSourceService;
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
  @Inject private BuildSourceService buildSourceService;

  public boolean validate(SettingAttribute settingAttribute) {
    SettingValue settingValue = settingAttribute.getValue();

    if (settingValue instanceof GcpConfig) {
      gcpHelperService.validateCredential(((GcpConfig) settingValue).getServiceAccountKeyFileContent());
    } else if (settingValue instanceof AwsConfig) {
      awsHelperService.validateCredential(
          ((AwsConfig) settingValue).getAccessKey(), ((AwsConfig) settingValue).getSecretKey());
    } else if (settingValue instanceof JenkinsConfig || settingValue instanceof BambooConfig
        || settingValue instanceof NexusConfig) {
      // TODO:: fix it to show appropriate error message instead of what BuildServer throws
      buildSourceService.getBuildService(settingAttribute, Base.GLOBAL_APP_ID).getJobs(settingValue);
    } else if (settingValue instanceof DockerConfig) {
      // figure out a way to validate Docker registry without image
    }
    return true;
  }
}
