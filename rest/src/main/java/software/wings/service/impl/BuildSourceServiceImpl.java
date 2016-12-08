package software.wings.service.impl;

import static software.wings.utils.Validator.notNullCheck;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.BambooConfig;
import software.wings.beans.ErrorCodes;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.BambooBuildService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.JenkinsBuildService;
import software.wings.service.intfc.SettingsService;

import java.util.List;
import java.util.Map;
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
  @Inject private BambooBuildService bambooBuildService;
  @Inject private SettingsService settingsService;

  @Override
  public Set<String> getJobs(String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return (settingAttribute.getValue() instanceof BambooConfig)
        ? bambooBuildService.getJobs((BambooConfig) settingAttribute.getValue())
        : jenkinsBuildService.getJobs((JenkinsConfig) settingAttribute.getValue());
  }

  @Override
  public Map<String, String> getPlans(@NotEmpty String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    if (!(settingAttribute.getValue() instanceof BambooConfig)) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message",
          "Unsupported operation for setting type " + settingAttribute.getValue().getType());
    }
    return bambooBuildService.getPlans((BambooConfig) settingAttribute.getValue());
  }

  @Override
  public Set<String> getArtifactPaths(String jobName, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return (settingAttribute.getValue() instanceof BambooConfig)
        ? bambooBuildService.getArtifactPaths(jobName, (BambooConfig) settingAttribute.getValue())
        : jenkinsBuildService.getArtifactPaths(jobName, (JenkinsConfig) settingAttribute.getValue());
  }

  @Override
  public List<BuildDetails> getBuilds(String appId, String artifactStreamId, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return (settingAttribute.getValue() instanceof BambooConfig)
        ? bambooBuildService.getBuilds(appId, artifactStreamId, (BambooConfig) settingAttribute.getValue())
        : jenkinsBuildService.getBuilds(appId, artifactStreamId, (JenkinsConfig) settingAttribute.getValue());
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, String artifactStreamId, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return (settingAttribute.getValue() instanceof BambooConfig)
        ? bambooBuildService.getLastSuccessfulBuild(appId, artifactStreamId, (BambooConfig) settingAttribute.getValue())
        : jenkinsBuildService.getLastSuccessfulBuild(
              appId, artifactStreamId, (JenkinsConfig) settingAttribute.getValue());
  }
}
