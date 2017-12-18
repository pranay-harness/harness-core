package software.wings.service.impl;

import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.utils.Validator.equalCheck;

import software.wings.beans.AwsConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.ami.AmiService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.AmiBuildService;
import software.wings.utils.ArtifactType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by sgurubelli on 12/15/17.
 */
@Singleton
public class AmiBuildServiceImpl implements AmiBuildService {
  @Inject private AmiService amiService;

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.AMI.name());
    return amiService.getBuilds(
        awsConfig, encryptionDetails, artifactStreamAttributes.getRegion(), artifactStreamAttributes.getTags(), 50);
  }

  @Override
  public List<JobDetails> getJobs(
      AwsConfig jenkinsConfig, List<EncryptedDataDetail> encryptionDetails, Optional<String> parentJobName) {
    throw new WingsException(INVALID_REQUEST, "message", "Operation not supported by Ami Artifact Stream");
  }

  @Override
  public List<String> getArtifactPaths(
      String jobName, String groupId, AwsConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new WingsException(INVALID_REQUEST, "message", "Operation not supported by Ami Artifact Stream");
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      AwsConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new WingsException(INVALID_REQUEST, "message", "Operation not supported by Ami Artifact Stream");
  }

  @Override
  public Map<String, String> getPlans(AwsConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new WingsException(INVALID_REQUEST, "message", "Operation not supported by Ami Artifact Stream");
  }

  @Override
  public Map<String, String> getPlans(
      AwsConfig config, List<EncryptedDataDetail> encryptionDetails, ArtifactType artifactType, String repositoryType) {
    throw new WingsException(INVALID_REQUEST, "message", "Operation not supported by Ami Artifact Stream");
  }

  @Override
  public List<String> getGroupIds(String repoType, AwsConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new WingsException(INVALID_REQUEST, "message", "Operation not supported by Ami Artifact Stream");
  }

  @Override
  public boolean validateArtifactServer(AwsConfig config) {
    return true;
  }

  @Override
  public boolean validateArtifactSource(AwsConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) {
    return true;
  }
}