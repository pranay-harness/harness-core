package software.wings.service.impl;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Sets;

import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
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
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ServiceLocator serviceLocator;

  @Override
  public Set<JobDetails> getJobs(String appId, String settingId, String parentJobName) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    List<JobDetails> jobs = getBuildService(settingAttribute, appId)
                                .getJobs(settingAttribute.getValue(), Optional.ofNullable(parentJobName));
    // Sorting the job details by name before returning
    TreeSet<JobDetails> jobDetailsSet =
        Sets.newTreeSet(Comparator.comparing(JobDetails::getJobName, String::compareToIgnoreCase));
    jobDetailsSet.addAll(jobs);
    return jobDetailsSet;
  }

  @Override
  public Map<String, String> getPlans(String appId, String settingId, String artifactStreamType) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return getBuildService(settingAttribute, appId, artifactStreamType).getPlans(settingAttribute.getValue());
  }

  @Override
  public Map<String, String> getPlans(String appId, String settingId, String serviceId, String artifactStreamType) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    Service service = serviceResourceService.get(appId, serviceId);
    notNullCheck("Service", service);
    return getBuildService(settingAttribute, appId).getPlans(settingAttribute.getValue(), service.getArtifactType());
  }

  @Override
  public Set<String> getArtifactPaths(
      String appId, String jobName, String settingId, String groupId, String artifactStreamType) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return Sets.newTreeSet(getBuildService(settingAttribute, appId, artifactStreamType)
                               .getArtifactPaths(jobName, groupId, settingAttribute.getValue()));
  }

  @Override
  public List<BuildDetails> getBuilds(String appId, String artifactStreamId, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);

    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    notNullCheck("Artifact Stream", artifactStream);
    Service service = serviceResourceService.get(appId, artifactStream.getServiceId());
    ArtifactStreamAttributes artifactStreamAttributes = artifactStream.getArtifactStreamAttributes();
    artifactStreamAttributes.setArtifactType(service.getArtifactType());
    return getBuildService(settingAttribute, appId)
        .getBuilds(appId, artifactStreamAttributes, settingAttribute.getValue());
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, String artifactStreamId, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    notNullCheck("Artifact Stream", artifactStream);
    Service service = serviceResourceService.get(appId, artifactStream.getServiceId());
    ArtifactStreamAttributes artifactStreamAttributes = artifactStream.getArtifactStreamAttributes();
    artifactStreamAttributes.setArtifactType(service.getArtifactType());
    String artifactStreamType = artifactStream.getArtifactStreamType();
    if (ArtifactStreamType.AMAZON_S3.getName().equals(artifactStreamType)) {
      return getBuildService(settingAttribute, appId, artifactStreamType)
          .getLastSuccessfulBuild(appId, artifactStreamAttributes, settingAttribute.getValue());
    } else {
      return getBuildService(settingAttribute, appId)
          .getLastSuccessfulBuild(appId, artifactStreamAttributes, settingAttribute.getValue());
    }
  }

  @Override
  public Set<String> getGroupIds(String appId, String repoType, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return Sets.newTreeSet(getBuildService(settingAttribute, appId).getGroupIds(repoType, settingAttribute.getValue()));
  }

  @Override
  public boolean validateArtifactSource(
      String appId, String settingId, ArtifactStreamAttributes artifactStreamAttributes) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("Setting", settingAttribute);
    return getBuildService(settingAttribute, appId)
        .validateArtifactSource(settingAttribute.getValue(), artifactStreamAttributes);
  }

  @Override
  public BuildService getBuildService(SettingAttribute settingAttribute, String appId) {
    SyncTaskContext syncTaskContext =
        aContext().withAccountId(settingAttribute.getAccountId()).withAppId(appId).build();
    return delegateProxyFactory.get(buildServiceMap.get(settingAttribute.getValue().getClass()), syncTaskContext);
  }

  private BuildService getBuildService(SettingAttribute settingAttribute, String appId, String artifactStreamType) {
    if (artifactStreamType == null) {
      return getBuildService(settingAttribute, appId);
    }
    Class<? extends BuildService> buildServiceClass = serviceLocator.getBuildServiceClass(artifactStreamType);
    SyncTaskContext syncTaskContext =
        aContext().withAccountId(settingAttribute.getAccountId()).withAppId(appId).build();
    return delegateProxyFactory.get(buildServiceClass, syncTaskContext);
  }
}
