package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.validation.Validator.equalCheck;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.AzureConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.helpers.ext.azure.AcrService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.service.intfc.AcrBuildService;
import software.wings.utils.ArtifactType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@OwnedBy(CDC)
@Singleton
public class AcrBuildServiceImpl implements AcrBuildService {
  @Inject private AcrService acrService;

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.ACR.name());
    return wrapNewBuildsWithLabels(acrService.getBuilds(azureConfig, encryptionDetails, artifactStreamAttributes, 50),
        artifactStreamAttributes, azureConfig, encryptionDetails);
  }

  @Override
  public List<JobDetails> getJobs(
      AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails, Optional<String> parentJobName) {
    return Lists.newArrayList();
  }

  @Override
  public List<String> getArtifactPaths(
      String subscriptionId, String groupId, AzureConfig config, List<EncryptedDataDetail> encryptionDetails) {
    return acrService.listRegistries(config, encryptionDetails, subscriptionId);
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      AzureConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by ACR Artifact Stream");
  }

  @Override
  public Map<String, String> getPlans(AzureConfig config, List<EncryptedDataDetail> encryptionDetails) {
    return getJobs(config, encryptionDetails, Optional.empty())
        .stream()
        .collect(Collectors.toMap(JobDetails::getJobName, JobDetails::getJobName));
  }

  @Override
  public Map<String, String> getPlans(AzureConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactType artifactType, String reposiotoryType) {
    return getPlans(config, encryptionDetails);
  }

  @Override
  public List<String> getGroupIds(String jobName, AzureConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by ACR Artifact Stream");
  }

  @Override
  public boolean validateArtifactSource(AzureConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) {
    return acrService.verifyImageName(config, encryptionDetails, artifactStreamAttributes);
  }

  @Override
  public boolean validateArtifactServer(AzureConfig config, List<EncryptedDataDetail> encryptedDataDetails) {
    return true;
  }

  @Override
  public Map<String, String> getBuckets(
      AzureConfig config, String projectId, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by ACR Artifact Stream");
  }

  @Override
  public List<String> getSmbPaths(AzureConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by Azure Build Service", WingsException.USER);
  }

  @Override
  public List<String> getArtifactPathsByStreamType(
      AzureConfig config, List<EncryptedDataDetail> encryptionDetails, String streamType) {
    throw new InvalidRequestException("Operation not supported by Azure Build Service", WingsException.USER);
  }
}
