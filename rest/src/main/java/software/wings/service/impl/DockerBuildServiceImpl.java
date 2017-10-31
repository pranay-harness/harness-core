package software.wings.service.impl;

import static software.wings.utils.HttpUtil.connectableHttpUrl;
import static software.wings.utils.HttpUtil.validUrl;
import static software.wings.utils.Validator.equalCheck;

import software.wings.beans.DockerConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.docker.DockerRegistryService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.DockerBuildService;
import software.wings.utils.ArtifactType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by anubhaw on 1/6/17.
 */
@Singleton
public class DockerBuildServiceImpl implements DockerBuildService {
  @Inject private DockerRegistryService dockerRegistryService;

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.DOCKER.name());
    List<BuildDetails> builds =
        dockerRegistryService.getBuilds(dockerConfig, encryptionDetails, artifactStreamAttributes.getImageName(), 50);
    return builds;
  }

  @Override
  public List<JobDetails> getJobs(
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails, Optional<String> parentJobName) {
    throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Operation not supported by Docker Artifact Stream");
  }

  @Override
  public List<String> getArtifactPaths(
      String jobName, String groupId, DockerConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Operation not supported by Docker Artifact Stream");
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails) {
    throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Operation not supported by Docker Artifact Stream");
  }

  @Override
  public Map<String, String> getPlans(DockerConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Operation not supported by Docker Artifact Stream");
  }

  @Override
  public Map<String, String> getPlans(DockerConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactType artifactType, String repositoryType) {
    return getPlans(config, encryptionDetails);
  }

  @Override
  public List<String> getGroupIds(String jobName, DockerConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Operation not supported by Docker Artifact Stream");
  }

  @Override
  public boolean validateArtifactServer(DockerConfig config) {
    if (!validUrl(config.getDockerRegistryUrl())) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Docker Registry URL must be a valid URL");
    }
    if (!connectableHttpUrl(config.getDockerRegistryUrl())) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message",
          "Could not reach Docker Registry at : " + config.getDockerRegistryUrl());
    }
    return dockerRegistryService.validateCredentials(config, Collections.emptyList());
  }

  @Override
  public boolean validateArtifactSource(DockerConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) {
    return dockerRegistryService.verifyImageName(config, encryptionDetails, artifactStreamAttributes.getImageName());
  }
}
