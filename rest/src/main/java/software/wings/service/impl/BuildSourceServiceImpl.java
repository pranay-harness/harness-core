package software.wings.service.impl;

import static software.wings.utils.Validator.notNullCheck;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.SettingAttribute;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.BuildService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue;

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
  @Inject private Map<Class<? extends SettingValue>, BuildService> buildServiceMap;

  @Inject private SettingsService settingsService;

  @Override
  public Set<String> getJobs(String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return buildServiceMap.get(settingAttribute.getValue().getClass()).getJobs(settingAttribute.getValue());
  }

  @Override
  public Map<String, String> getPlans(@NotEmpty String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return buildServiceMap.get(settingAttribute.getValue().getClass()).getPlans(settingAttribute.getValue());
  }

  @Override
  public Set<String> getArtifactPaths(String jobName, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return buildServiceMap.get(settingAttribute.getValue().getClass())
        .getArtifactPaths(jobName, settingAttribute.getValue());
  }

  @Override
  public List<BuildDetails> getBuilds(String appId, String artifactStreamId, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return buildServiceMap.get(settingAttribute.getValue().getClass())
        .getBuilds(appId, artifactStreamId, settingAttribute.getValue());
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, String artifactStreamId, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return buildServiceMap.get(settingAttribute.getValue().getClass())
        .getLastSuccessfulBuild(appId, artifactStreamId, settingAttribute.getValue());
  }
}
