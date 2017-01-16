package software.wings.service.impl;

import static software.wings.beans.DelegateTask.Context.Builder.aContext;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Sets;

import software.wings.beans.DelegateTask.Context;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.ArtifactStreamService;
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
  @Inject private Map<Class<? extends SettingValue>, Class<? extends BuildService>> buildServiceMap;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private SettingsService settingsService;

  @Override
  public Set<String> getJobs(String appId, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return Sets.newHashSet(getBuildService(settingAttribute, appId).getJobs(settingAttribute.getValue()));
  }

  @Override
  public Map<String, String> getPlans(String appId, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return getBuildService(settingAttribute, appId).getPlans(settingAttribute.getValue());
  }

  @Override
  public Set<String> getArtifactPaths(String appId, String jobName, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return Sets.newHashSet(
        getBuildService(settingAttribute, appId).getArtifactPaths(jobName, settingAttribute.getValue()));
  }

  @Override
  public List<BuildDetails> getBuilds(String appId, String artifactStreamId, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    String jobName = getJobName(appId, artifactStreamId);
    return getBuildService(settingAttribute, appId).getBuilds(appId, jobName, settingAttribute.getValue());
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, String artifactStreamId, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    String jobName = getJobName(appId, artifactStreamId);
    return getBuildService(settingAttribute, appId).getLastSuccessfulBuild(appId, jobName, settingAttribute.getValue());
  }

  private String getJobName(String appId, String artifactStreamId) {
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    return artifactStream.getJobname();
  }

  private BuildService getBuildService(SettingAttribute settingAttribute, String appId) {
    Context context = aContext().withAccountId(settingAttribute.getAccountId()).withAppId(appId).build();
    return delegateProxyFactory.get(buildServiceMap.get(settingAttribute.getValue().getClass()), context);
  }
}
