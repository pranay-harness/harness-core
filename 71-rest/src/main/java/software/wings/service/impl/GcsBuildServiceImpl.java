package software.wings.service.impl;

import static io.harness.validation.Validator.equalCheck;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.GcpConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.helpers.ext.gcs.GcsService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.service.intfc.GcsBuildService;
import software.wings.utils.ArtifactType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class GcsBuildServiceImpl implements GcsBuildService {
  @Inject private GcsService gcsService;

  @Override
  public Map<String, String> getBuckets(
      GcpConfig gcpConfig, String projectId, List<EncryptedDataDetail> encryptionDetails) {
    return gcsService.listBuckets(gcpConfig, projectId, encryptionDetails);
  }

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.GCS.name());
    return wrapNewBuildsWithLabels(getBuilds(appId, artifactStreamAttributes, gcpConfig, encryptionDetails, 100),
        artifactStreamAttributes, gcpConfig, encryptionDetails);
  }

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, int limit) {
    String artifactName = artifactStreamAttributes.getArtifactName();
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.GCS.name());
    return wrapNewBuildsWithLabels(
        gcsService.getArtifactsBuildDetails(gcpConfig, encryptionDetails, artifactStreamAttributes,
            Lists.newArrayList(artifactName), artifactName.contains("*"), limit),
        artifactStreamAttributes, gcpConfig, encryptionDetails);
  }

  @Override
  public List<JobDetails> getJobs(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, Optional<String> parentJobName) {
    return null;
  }

  @Override
  public List<String> getArtifactPaths(
      String bucketName, String groupId, GcpConfig config, List<EncryptedDataDetail> encryptionDetails) {
    return gcsService.getArtifactPaths(config, encryptionDetails, bucketName);
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      GcpConfig config, List<EncryptedDataDetail> encryptionDetails) {
    return null;
  }

  @Override
  public List<String> getGroupIds(String repoType, GcpConfig config, List<EncryptedDataDetail> encryptionDetails) {
    return null;
  }

  @Override
  public boolean validateArtifactServer(GcpConfig config, List<EncryptedDataDetail> encryptionDetails) {
    return false;
  }

  @Override
  public boolean validateArtifactSource(GcpConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) {
    return false;
  }

  @Override
  public Map<String, String> getPlans(GcpConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by GCS Artifact Stream");
  }

  @Override
  public Map<String, String> getPlans(
      GcpConfig config, List<EncryptedDataDetail> encryptionDetails, ArtifactType artifactType, String repositoryType) {
    throw new InvalidRequestException("Operation not supported by GCS Artifact Stream");
  }

  @Override
  public List<String> getSmbPaths(GcpConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by GCS Build Service", WingsException.USER);
  }

  @Override
  public List<String> getArtifactPathsByStreamType(
      GcpConfig config, List<EncryptedDataDetail> encryptionDetails, String streamType) {
    throw new InvalidRequestException("Operation not supported by GCS Build Service", WingsException.USER);
  }
}
