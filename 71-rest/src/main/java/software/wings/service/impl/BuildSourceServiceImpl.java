package software.wings.service.impl;

import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.exception.WingsException.USER;
import static software.wings.beans.DelegateTask.DEFAULT_SYNC_CALL_TIMEOUT;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
import static software.wings.beans.artifact.ArtifactStreamType.AMI;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.CUSTOM;
import static software.wings.beans.artifact.ArtifactStreamType.GCS;
import static software.wings.beans.artifact.ArtifactStreamType.SMB;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.GcpConfig;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.helpers.ext.gcs.GcsService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactCollectionService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.artifact.CustomBuildSourceService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.ArrayList;
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
  @Inject @Named("ArtifactCollectionService") private ArtifactCollectionService artifactCollectionService;
  @Inject @Named("AsyncArtifactCollectionService") private ArtifactCollectionService artifactCollectionServiceAsync;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private AppService appService;
  @Inject private GcsService gcsService;
  @Inject private CustomBuildSourceService customBuildSourceService;
  private static final Logger logger = LoggerFactory.getLogger(BuildSourceServiceImpl.class);

  @Override
  public Set<JobDetails> getJobs(String appId, String settingId, String parentJobName) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    SettingValue settingValue = getSettingValue(settingAttribute);
    List<EncryptedDataDetail> encryptedDataDetails = getEncryptedDataDetails((EncryptableSetting) settingValue);
    List<JobDetails> jobs = getBuildService(settingAttribute, appId)
                                .getJobs(settingValue, encryptedDataDetails, Optional.ofNullable(parentJobName));
    // Sorting the job details by name before returning
    Set<JobDetails> jobDetailsSet =
        Sets.newTreeSet(Comparator.comparing(JobDetails::getJobName, String::compareToIgnoreCase));
    jobDetailsSet.addAll(jobs);
    return jobDetailsSet;
  }

  @Override
  public String getProject(String appId, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    if (settingAttribute == null) {
      throw new WingsException(GENERAL_ERROR, USER)
          .addParam("message", "GCP Cloud provider Settings Attribute is null");
    }
    SettingValue settingValue = settingAttribute.getValue();
    List<EncryptedDataDetail> encryptedDataDetails = getEncryptedDataDetails((EncryptableSetting) settingValue);

    GcpConfig gcpConfig = (GcpConfig) settingValue;
    return gcsService.getProject(gcpConfig, encryptedDataDetails);
  }

  @Override
  public Map<String, String> getBuckets(String appId, String projectId, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    SettingValue settingValue = getSettingValue(settingAttribute);
    return getBuildService(settingAttribute, appId, GCS.name())
        .getBuckets(
            getSettingValue(settingAttribute), projectId, getEncryptedDataDetails((EncryptableSetting) settingValue));
  }

  @Override
  public List<String> getArtifactPathsByStreamType(String appId, String settingId, String streamType) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    SettingValue settingValue = getSettingValue(settingAttribute);
    return getBuildService(settingAttribute, appId, streamType)
        .getArtifactPathsByStreamType(
            getSettingValue(settingAttribute), getEncryptedDataDetails((EncryptableSetting) settingValue), streamType);
  }

  @Override
  public List<String> getSmbPaths(String appId, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    SettingValue settingValue = getSettingValue(settingAttribute);
    return getBuildService(settingAttribute, appId, SMB.name())
        .getSmbPaths(getSettingValue(settingAttribute), getEncryptedDataDetails((EncryptableSetting) settingValue));
  }

  @Override
  public Map<String, String> getPlans(String appId, String settingId, String artifactStreamType) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    SettingValue settingValue = getSettingValue(settingAttribute);
    return getBuildService(settingAttribute, appId, artifactStreamType)
        .getPlans(getSettingValue(settingAttribute), getEncryptedDataDetails((EncryptableSetting) settingValue));
  }

  @Override
  public Map<String, String> getPlans(
      String appId, String settingId, String serviceId, String artifactStreamType, String repositoryType) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    SettingValue value = getSettingValue(settingAttribute);
    Service service = serviceResourceService.get(appId, serviceId, false);
    notNullCheck("Service", service);
    List<EncryptedDataDetail> encryptedDataDetails = getEncryptedDataDetails((EncryptableSetting) value);
    return getBuildService(settingAttribute, appId)
        .getPlans(value, encryptedDataDetails, service.getArtifactType(), repositoryType);
  }

  @Override
  public Set<String> getArtifactPaths(
      String appId, String jobName, String settingId, String groupId, String artifactStreamType) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    SettingValue value = getSettingValue(settingAttribute);
    List<EncryptedDataDetail> encryptedDataDetails = getEncryptedDataDetails((EncryptableSetting) value);
    return Sets.newTreeSet(getBuildService(settingAttribute, appId, artifactStreamType)
                               .getArtifactPaths(jobName, groupId, value, encryptedDataDetails));
  }

  @Override
  public List<BuildDetails> getBuilds(String appId, String artifactStreamId, String settingId) {
    return getBuilds(appId, artifactStreamId, settingId, -1);
  }

  @Override
  public List<BuildDetails> getBuilds(String appId, String artifactStreamId, String settingId, int limit) {
    ArtifactStream artifactStream = getArtifactStream(appId, artifactStreamId);
    if (CUSTOM.name().equals(artifactStream.getArtifactStreamType())) {
      return customBuildSourceService.getBuilds(appId, artifactStreamId);
    }
    SettingAttribute settingAttribute = settingsService.get(settingId);
    if (settingAttribute == null) {
      logger.warn("Artifact Server {} was deleted of artifactStreamId {}", settingId, artifactStreamId);
      return new ArrayList<>();
    }
    SettingValue settingValue = getSettingValue(settingAttribute);
    List<EncryptedDataDetail> encryptedDataDetails = getEncryptedDataDetails((EncryptableSetting) settingValue);

    Service service = getService(appId, artifactStream);
    String artifactStreamType = artifactStream.getArtifactStreamType();

    ArtifactStreamAttributes artifactStreamAttributes = getArtifactStreamAttributes(artifactStream, service);

    return getBuildDetails(appId, limit, settingAttribute, settingValue, encryptedDataDetails, artifactStreamType,
        artifactStreamAttributes);
  }

  protected List<BuildDetails> getBuildDetails(String appId, int limit, SettingAttribute settingAttribute,
      SettingValue settingValue, List<EncryptedDataDetail> encryptedDataDetails, String artifactStreamType,
      ArtifactStreamAttributes artifactStreamAttributes) {
    if (limit != -1 && ARTIFACTORY.name().equals(artifactStreamType)) {
      // TODO: The limit supported only for Artifactory for now
      return getBuildService(settingAttribute, appId)
          .getBuilds(appId, artifactStreamAttributes, settingValue, encryptedDataDetails, limit);
    } else if (AMAZON_S3.name().equals(artifactStreamType) || AMI.name().equals(artifactStreamType)
        || GCS.name().equals(artifactStreamType)) {
      return getBuildService(settingAttribute, appId, artifactStreamType)
          .getBuilds(appId, artifactStreamAttributes, settingValue, encryptedDataDetails);
    } else {
      return getBuildService(settingAttribute, appId)
          .getBuilds(appId, artifactStreamAttributes, settingValue, encryptedDataDetails);
    }
  }

  private ArtifactStreamAttributes getArtifactStreamAttributes(ArtifactStream artifactStream, Service service) {
    ArtifactStreamAttributes artifactStreamAttributes = artifactStream.fetchArtifactStreamAttributes();
    artifactStreamAttributes.setArtifactType(service.getArtifactType());
    return artifactStreamAttributes;
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, String artifactStreamId, String settingId) {
    ArtifactStream artifactStream = getArtifactStream(appId, artifactStreamId);

    SettingAttribute settingAttribute = settingsService.get(settingId);

    SettingValue settingValue = getSettingValue(settingAttribute);
    List<EncryptedDataDetail> encryptedDataDetails = getEncryptedDataDetails((EncryptableSetting) settingValue);

    Service service = getService(appId, artifactStream);

    ArtifactStreamAttributes artifactStreamAttributes = getArtifactStreamAttributes(artifactStream, service);

    if (AMAZON_S3.name().equals(artifactStream.getArtifactStreamType())) {
      return getBuildService(settingAttribute, appId, artifactStream.getArtifactStreamType())
          .getLastSuccessfulBuild(appId, artifactStreamAttributes, settingValue, encryptedDataDetails);
    } else {
      return getBuildService(settingAttribute, appId)
          .getLastSuccessfulBuild(appId, artifactStreamAttributes, settingValue, encryptedDataDetails);
    }
  }

  private ArtifactStream getArtifactStream(String appId, String artifactStreamId) {
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    notNullCheck("Artifact Stream", artifactStream);
    return artifactStream;
  }

  private Service getService(String appId, ArtifactStream artifactStream) {
    Service service = serviceResourceService.get(appId, artifactStream.getServiceId(), false);
    notNullCheck("Service might have been deleted", service, USER);
    return service;
  }

  @Override
  public Set<String> getGroupIds(String appId, String repoType, String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    SettingValue settingValue = getSettingValue(settingAttribute);
    List<EncryptedDataDetail> encryptedDataDetails = getEncryptedDataDetails((EncryptableSetting) settingValue);
    return Sets.newTreeSet(
        getBuildService(settingAttribute, appId).getGroupIds(repoType, settingValue, encryptedDataDetails));
  }

  @Override
  public boolean validateArtifactSource(
      String appId, String settingId, ArtifactStreamAttributes artifactStreamAttributes) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    SettingValue settingValue = getSettingValue(settingAttribute);
    List<EncryptedDataDetail> encryptedDataDetails = getEncryptedDataDetails((EncryptableSetting) settingValue);
    return getBuildService(settingAttribute, appId)
        .validateArtifactSource(settingValue, encryptedDataDetails, artifactStreamAttributes);
  }

  @Override
  public boolean validateArtifactSource(ArtifactStream artifactStream) {
    return customBuildSourceService.validateArtifactSource(artifactStream);
  }

  @Override
  public JobDetails getJob(String appId, String settingId, String jobName) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    SettingValue settingValue = getSettingValue(settingAttribute);
    List<EncryptedDataDetail> encryptedDataDetails = getEncryptedDataDetails((EncryptableSetting) settingValue);
    return getBuildService(settingAttribute, appId).getJob(jobName, settingValue, encryptedDataDetails);
  }

  private SettingValue getSettingValue(SettingAttribute settingAttribute) {
    notNullCheck("Artifact server was deleted", settingAttribute, USER);
    return settingAttribute.getValue();
  }

  @Override
  public BuildService getBuildService(SettingAttribute settingAttribute, String appId) {
    SyncTaskContext syncTaskContext =
        SyncTaskContext.builder()
            .accountId(settingAttribute.getAccountId())
            .appId(appId)
            .timeout(settingAttribute.getValue().getType().equals(SettingVariableTypes.JENKINS.name())
                    ? 120 * 1000
                    : DEFAULT_SYNC_CALL_TIMEOUT)
            .build();
    return delegateProxyFactory.get(buildServiceMap.get(settingAttribute.getValue().getClass()), syncTaskContext);
  }

  @Override
  public Artifact collectArtifact(String appId, String artifactStreamId, BuildDetails buildDetails) {
    if (buildDetails == null) {
      throw new InvalidRequestException("Build details can not null", USER);
    }
    return artifactCollectionServiceAsync.collectArtifact(appId, artifactStreamId, buildDetails);
  }

  private BuildService getBuildService(SettingAttribute settingAttribute, String appId, String artifactStreamType) {
    if (artifactStreamType == null) {
      return getBuildService(settingAttribute, appId);
    }
    Class<? extends BuildService> buildServiceClass = serviceLocator.getBuildServiceClass(artifactStreamType);
    SyncTaskContext syncTaskContext =
        SyncTaskContext.builder().accountId(settingAttribute.getAccountId()).appId(appId).timeout(120 * 1000).build();
    return delegateProxyFactory.get(buildServiceClass, syncTaskContext);
  }

  private List<EncryptedDataDetail> getEncryptedDataDetails(EncryptableSetting settingValue) {
    return secretManager.getEncryptionDetails(settingValue, null, null);
  }
}
