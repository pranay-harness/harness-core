package software.wings.service.impl;

import static io.harness.exception.WingsException.SRE;
import static io.harness.exception.WingsException.USER;
import static io.harness.network.Http.connectableHttpUrl;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.utils.Validator.equalCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import software.wings.beans.DockerConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.docker.DockerRegistryService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.DockerBuildService;
import software.wings.utils.ArtifactType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by anubhaw on 1/6/17.
 */
@Singleton
public class DockerBuildServiceImpl implements DockerBuildService {
  @Inject private DockerRegistryService dockerRegistryService;

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), DOCKER.name());
    return dockerRegistryService.getBuilds(
        dockerConfig, encryptionDetails, artifactStreamAttributes.getImageName(), 50);
  }

  @Override
  public List<JobDetails> getJobs(
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails, Optional<String> parentJobName) {
    throw new InvalidRequestException("Operation not supported by Docker Artifact Stream", SRE);
  }

  @Override
  public List<String> getArtifactPaths(
      String jobName, String groupId, DockerConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by Docker Artifact Stream", SRE);
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by Docker Artifact Stream", SRE);
  }

  @Override
  public Map<String, String> getPlans(DockerConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by Docker Artifact Stream", SRE);
  }

  @Override
  public Map<String, String> getPlans(DockerConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactType artifactType, String repositoryType) {
    return getPlans(config, encryptionDetails);
  }

  @Override
  public List<String> getGroupIds(String jobName, DockerConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by Docker Artifact Stream", SRE);
  }

  @Override
  public boolean validateArtifactServer(DockerConfig config, List<EncryptedDataDetail> encryptedDataDetails) {
    if (!connectableHttpUrl(config.getDockerRegistryUrl())) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, USER)
          .addParam("message", "Could not reach Docker Registry at : " + config.getDockerRegistryUrl());
    }
    return dockerRegistryService.validateCredentials(config, encryptedDataDetails);
  }

  @Override
  public boolean validateArtifactSource(DockerConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) {
    return dockerRegistryService.verifyImageName(config, encryptionDetails, artifactStreamAttributes.getImageName());
  }

  @Override
  public Map<String, String> getBuckets(
      DockerConfig config, String projectId, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by Docker Artifact Stream", USER);
  }

  @Override
  public List<String> getSmbPaths(DockerConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by Docker Build Service", WingsException.USER);
  }
}
