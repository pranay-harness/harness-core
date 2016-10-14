package software.wings.service.impl;

import static software.wings.utils.Validator.notNullCheck;

import software.wings.beans.JenkinsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.JenkinsBuildService;
import software.wings.service.intfc.SettingsService;

import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 8/18/16.
 */
@ValidateOnExecution
@Singleton
public class BuildSourceServiceImpl implements BuildSourceService {
  @Inject private JenkinsBuildService jenkinsBuildService;
  @Inject private SettingsService settingsService;

  @Override
  public Set<String> getJobs(String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return jenkinsBuildService.getJobs((JenkinsConfig) settingAttribute.getValue());
  }

  @Override
  public Set<String> getArtifactPaths(String jobName, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return jenkinsBuildService.getArtifactPaths(jobName, (JenkinsConfig) settingAttribute.getValue());
  }

  @Override
  public List<BuildDetails> getBuilds(String artifactStreamId, String appId, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return jenkinsBuildService.getBuilds(artifactStreamId, appId, (JenkinsConfig) settingAttribute.getValue());
  }
}
