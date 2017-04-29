package software.wings.service.impl;

import static software.wings.beans.DelegateTask.Context.Builder.aContext;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Sets;

import software.wings.beans.DelegateTask.Context;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
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
  @Inject private SettingsService settingsService;
  @Inject private ArtifactStreamService artifactStreamService;

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
  public Set<String> getArtifactPaths(String appId, String jobName, String settingId, String groupId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return Sets.newHashSet(
        getBuildService(settingAttribute, appId).getArtifactPaths(jobName, groupId, settingAttribute.getValue()));
  }

  @Override
  public List<BuildDetails> getBuilds(String appId, String artifactStreamId, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);

    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    notNullCheck("Artifact Stream", artifactStream);

    return getBuildService(settingAttribute, appId)
        .getBuilds(appId, artifactStream.getArtifactStreamAttributes(), settingAttribute.getValue());
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, String artifactStreamId, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    notNullCheck("Artifact Stream", artifactStream);
    return getBuildService(settingAttribute, appId)
        .getLastSuccessfulBuild(appId, artifactStream.getArtifactStreamAttributes(), settingAttribute.getValue());
  }

  @Override
  public Set<String> getGroupIds(String appId, String repoType, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return Sets.newHashSet(getBuildService(settingAttribute, appId).getGroupIds(repoType, settingAttribute.getValue()));
  }

  @Override
  public boolean validateArtifactSource(
      String appId, String settingId, ArtifactStreamAttributes artifactStreamAttributes) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return getBuildService(settingAttribute, appId)
        .validateArtifactSource(settingAttribute.getValue(), artifactStreamAttributes);
  }

  private BuildService getBuildService(SettingAttribute settingAttribute, String appId) {
    Context context = aContext().withAccountId(settingAttribute.getAccountId()).withAppId(appId).build();
    return delegateProxyFactory.get(buildServiceMap.get(settingAttribute.getValue().getClass()), context);
  }
}
