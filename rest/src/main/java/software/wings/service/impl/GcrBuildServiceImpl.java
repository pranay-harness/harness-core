package software.wings.service.impl;

import static io.harness.exception.WingsException.USER;
import static software.wings.utils.Validator.equalCheck;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.GcpConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.exception.InvalidRequestException;
import software.wings.helpers.ext.gcr.GcrService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.GcrBuildService;
import software.wings.utils.ArtifactType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by brett on 8/2/17
 */
@Singleton
public class GcrBuildServiceImpl implements GcrBuildService {
  @Inject private GcrService gcrService;

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.GCR.name());
    return gcrService.getBuilds(gcpConfig, encryptionDetails, artifactStreamAttributes, 50);
  }

  @Override
  public List<JobDetails> getJobs(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, Optional<String> parentJobName) {
    return Lists.newArrayList();
  }

  @Override
  public List<String> getArtifactPaths(
      String jobName, String groupId, GcpConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by GCR Artifact Stream", USER);
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by GCR Artifact Stream", USER);
  }

  @Override
  public Map<String, String> getPlans(GcpConfig config, List<EncryptedDataDetail> encryptionDetails) {
    return getJobs(config, encryptionDetails, Optional.empty())
        .stream()
        .collect(Collectors.toMap(JobDetails::getJobName, JobDetails::getJobName));
  }

  @Override
  public Map<String, String> getPlans(GcpConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactType artifactType, String reposiotoryType) {
    return getPlans(config, encryptionDetails);
  }

  @Override
  public List<String> getGroupIds(String jobName, GcpConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by GCR Artifact Stream", USER);
  }

  @Override
  public boolean validateArtifactSource(GcpConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) {
    return gcrService.verifyImageName(config, encryptionDetails, artifactStreamAttributes);
  }

  @Override
  public boolean validateArtifactServer(GcpConfig config) {
    return true;
  }

  @Override
  public Map<String, String> getBuckets(
      GcpConfig config, String projectId, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by GCR Artifact Stream", USER);
  }
}
