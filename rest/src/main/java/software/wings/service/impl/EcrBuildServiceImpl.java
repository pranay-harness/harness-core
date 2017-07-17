package software.wings.service.impl;

import static software.wings.utils.HttpUtil.connectableHttpUrl;
import static software.wings.utils.HttpUtil.validUrl;
import static software.wings.utils.Validator.equalCheck;

import software.wings.beans.EcrConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.ecr.EcrService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.EcrBuildService;
import software.wings.utils.ArtifactType;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by brett on 7/16/17.
 */
@Singleton
public class EcrBuildServiceImpl implements EcrBuildService {
  @Inject private EcrService ecrService;

  @Override
  public List<BuildDetails> getBuilds(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, EcrConfig ecrConfig) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.ECR.name());
    List<BuildDetails> builds = ecrService.getBuilds(ecrConfig, artifactStreamAttributes.getImageName(), 50);
    return builds;
  }

  @Override
  public List<String> getJobs(EcrConfig ecrConfig) {
    return ecrService.listEcrRegistry(ecrConfig);
  }

  @Override
  public List<String> getArtifactPaths(String jobName, String groupId, EcrConfig config) {
    throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Operation not supported by Docker Artifact Stream");
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, EcrConfig ecrConfig) {
    throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Operation not supported by Docker Artifact Stream");
  }

  @Override
  public Map<String, String> getPlans(EcrConfig config) {
    throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Operation not supported by Docker Artifact Stream");
  }

  @Override
  public Map<String, String> getPlans(EcrConfig config, ArtifactType artifactType) {
    return getPlans(config);
  }

  @Override
  public List<String> getGroupIds(String jobName, EcrConfig config) {
    throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Operation not supported by Docker Artifact Stream");
  }

  @Override
  public boolean validateArtifactServer(EcrConfig config) {
    if (!validUrl(config.getEcrUrl())) {
      throw new WingsException(
          ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Amazon EC2 Container Registry URL must be a valid URL");
    }
    if (!connectableHttpUrl(config.getEcrUrl())) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message",
          "Could not reach Amazon EC2 Container Registry at : " + config.getEcrUrl());
    }
    return ecrService.validateCredentials(config);
  }

  @Override
  public boolean validateArtifactSource(EcrConfig config, ArtifactStreamAttributes artifactStreamAttributes) {
    return ecrService.verifyRepository(config, artifactStreamAttributes.getImageName());
  }
}
