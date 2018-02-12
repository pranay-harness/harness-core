package software.wings.service.impl;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
import static software.wings.beans.artifact.ArtifactStreamType.AMI;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.annotation.Encryptable;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
  @Inject private ServiceClassLocator serviceLocator;
  @Inject private SecretManager secretManager;

  @Override
  public Set<JobDetails> getJobs(String appId, String settingId, String parentJobName) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    SettingValue settingValue = getSettingValue(settingAttribute);
    List<EncryptedDataDetail> encryptedDataDetails = getEncryptedDataDetails((Encryptable) settingValue);
    List<JobDetails> jobs = getBuildService(settingAttribute, appId)
                                .getJobs(settingValue, encryptedDataDetails, Optional.ofNullable(parentJobName));
    // Sorting the job details by name before returning
    Set<JobDetails> jobDetailsSet =
        Sets.newTreeSet(Comparator.comparing(JobDetails::getJobName, String::compareToIgnoreCase));
    jobDetailsSet.addAll(jobs);
    return jobDetailsSet;
  }

  @Override
  public Map<String, String> getPlans(String appId, String settingId, String artifactStreamType) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    SettingValue settingValue = getSettingValue(settingAttribute);
    return getBuildService(settingAttribute, appId, artifactStreamType)
        .getPlans(getSettingValue(settingAttribute), getEncryptedDataDetails((Encryptable) settingValue));
  }

  @Override
  public Map<String, String> getPlans(
      String appId, String settingId, String serviceId, String artifactStreamType, String repositoryType) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    SettingValue value = getSettingValue(settingAttribute);
    Service service = serviceResourceService.get(appId, serviceId, false);
    notNullCheck("Service", service);
    List<EncryptedDataDetail> encryptedDataDetails = getEncryptedDataDetails((Encryptable) value);
    return getBuildService(settingAttribute, appId)
        .getPlans(value, encryptedDataDetails, service.getArtifactType(), repositoryType);
  }

  @Override
  public Set<String> getArtifactPaths(
      String appId, String jobName, String settingId, String groupId, String artifactStreamType) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    SettingValue value = getSettingValue(settingAttribute);
    List<EncryptedDataDetail> encryptedDataDetails = getEncryptedDataDetails((Encryptable) value);
    return Sets.newTreeSet(getBuildService(settingAttribute, appId, artifactStreamType)
                               .getArtifactPaths(jobName, groupId, value, encryptedDataDetails));
  }

  @Override
  public List<BuildDetails> getBuilds(String appId, String artifactStreamId, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    SettingValue settingValue = getSettingValue(settingAttribute);
    List<EncryptedDataDetail> encryptedDataDetails = getEncryptedDataDetails((Encryptable) settingValue);

    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    notNullCheck("Artifact Stream", artifactStream);
    Service service = serviceResourceService.get(appId, artifactStream.getServiceId(), false);
    ArtifactStreamAttributes artifactStreamAttributes = artifactStream.getArtifactStreamAttributes();
    artifactStreamAttributes.setArtifactType(service.getArtifactType());
    String artifactStreamType = artifactStream.getArtifactStreamType();

    if (AMAZON_S3.getName().equals(artifactStreamType) || AMI.getName().equals(artifactStreamType)) {
      return getBuildService(settingAttribute, appId, artifactStreamType)
          .getBuilds(appId, artifactStreamAttributes, settingValue, encryptedDataDetails);
    } else {
      return getBuildService(settingAttribute, appId)
          .getBuilds(appId, artifactStreamAttributes, settingValue, encryptedDataDetails);
    }
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, String artifactStreamId, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    SettingValue settingValue = getSettingValue(settingAttribute);
    List<EncryptedDataDetail> encryptedDataDetails = getEncryptedDataDetails((Encryptable) settingValue);

    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    notNullCheck("Artifact Stream", artifactStream);
    Service service = serviceResourceService.get(appId, artifactStream.getServiceId());
    ArtifactStreamAttributes artifactStreamAttributes = artifactStream.getArtifactStreamAttributes();
    artifactStreamAttributes.setArtifactType(service.getArtifactType());
    String artifactStreamType = artifactStream.getArtifactStreamType();

    if (AMAZON_S3.getName().equals(artifactStreamType)) {
      return getBuildService(settingAttribute, appId, artifactStreamType)
          .getLastSuccessfulBuild(appId, artifactStreamAttributes, settingValue, encryptedDataDetails);
    } else {
      return getBuildService(settingAttribute, appId)
          .getLastSuccessfulBuild(appId, artifactStreamAttributes, settingValue, encryptedDataDetails);
    }
  }

  @Override
  public Set<String> getGroupIds(String appId, String repoType, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    SettingValue settingValue = getSettingValue(settingAttribute);
    List<EncryptedDataDetail> encryptedDataDetails = getEncryptedDataDetails((Encryptable) settingValue);
    return Sets.newTreeSet(
        getBuildService(settingAttribute, appId).getGroupIds(repoType, settingValue, encryptedDataDetails));
  }

  @Override
  public boolean validateArtifactSource(
      String appId, String settingId, ArtifactStreamAttributes artifactStreamAttributes) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    SettingValue settingValue = getSettingValue(settingAttribute);
    List<EncryptedDataDetail> encryptedDataDetails = getEncryptedDataDetails((Encryptable) settingValue);
    return getBuildService(settingAttribute, appId)
        .validateArtifactSource(settingValue, encryptedDataDetails, artifactStreamAttributes);
  }

  @Override
  public JobDetails getJob(String appId, String settingId, String jobName) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    SettingValue settingValue = getSettingValue(settingAttribute);
    List<EncryptedDataDetail> encryptedDataDetails = getEncryptedDataDetails((Encryptable) settingValue);
    return getBuildService(settingAttribute, appId).getJob(jobName, settingValue, encryptedDataDetails);
  }

  private SettingValue getSettingValue(SettingAttribute settingAttribute) {
    notNullCheck("Setting", settingAttribute);
    return settingAttribute.getValue();
  }

  @Override
  public BuildService getBuildService(SettingAttribute settingAttribute, String appId) {
    SyncTaskContext syncTaskContext =
        aContext().withAccountId(settingAttribute.getAccountId()).withAppId(appId).build();
    if (settingAttribute.getValue().getType().equals(SettingVariableTypes.JENKINS.name())) {
      syncTaskContext.setTimeout(120 * 1000);
    }
    return delegateProxyFactory.get(buildServiceMap.get(settingAttribute.getValue().getClass()), syncTaskContext);
  }

  private BuildService getBuildService(SettingAttribute settingAttribute, String appId, String artifactStreamType) {
    if (artifactStreamType == null) {
      return getBuildService(settingAttribute, appId);
    }
    Class<? extends BuildService> buildServiceClass = serviceLocator.getBuildServiceClass(artifactStreamType);
    SyncTaskContext syncTaskContext =
        aContext().withAccountId(settingAttribute.getAccountId()).withAppId(appId).build();
    if (artifactStreamType.equals(ArtifactStreamType.JENKINS.name())) {
      syncTaskContext.setTimeout(120 * 1000);
    }
    return delegateProxyFactory.get(buildServiceClass, syncTaskContext);
  }

  private List<EncryptedDataDetail> getEncryptedDataDetails(Encryptable settingValue) {
    return secretManager.getEncryptionDetails(settingValue, null, null);
  }
}
