package software.wings.service.impl;

import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.AwsConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.Base;
import software.wings.beans.DockerConfig;
import software.wings.beans.ElkConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.LogzConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
import software.wings.settings.SettingValue;
import software.wings.sm.StateType;

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
  @Inject private AppdynamicsService appdynamicsService;
  @Inject private KubernetesHelperService kubernetesHelperService;
  @Inject private AnalysisService analysisService;

  public boolean validate(SettingAttribute settingAttribute) {
    SettingValue settingValue = settingAttribute.getValue();

    if (settingValue instanceof GcpConfig) {
      gcpHelperService.validateCredential(((GcpConfig) settingValue).getServiceAccountKeyFileContent());
    } else if (settingValue instanceof AwsConfig) {
      awsHelperService.validateAwsAccountCredential(
          ((AwsConfig) settingValue).getAccessKey(), ((AwsConfig) settingValue).getSecretKey());
    } else if (settingValue instanceof JenkinsConfig || settingValue instanceof BambooConfig
        || settingValue instanceof NexusConfig || settingValue instanceof DockerConfig
        || settingValue instanceof ArtifactoryConfig) {
      buildSourceService.getBuildService(settingAttribute, Base.GLOBAL_APP_ID).validateArtifactServer(settingValue);
    } else if (settingValue instanceof AppDynamicsConfig) {
      appdynamicsService.validateConfig(settingAttribute);
    } else if (settingValue instanceof KubernetesConfig) {
      kubernetesHelperService.validateCredential((KubernetesConfig) settingValue);
    } else if (settingValue instanceof SplunkConfig) {
      analysisService.validateConfig(settingAttribute, StateType.SPLUNKV2);
    } else if (settingValue instanceof ElkConfig) {
      analysisService.validateConfig(settingAttribute, StateType.ELK);
    } else if (settingValue instanceof LogzConfig) {
      analysisService.validateConfig(settingAttribute, StateType.LOGZ);
    }
    return true;
  }
}
