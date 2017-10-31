package software.wings.service;

import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.utils.HttpUtil.connectableHttpUrl;
import static software.wings.utils.HttpUtil.validUrl;
import static software.wings.utils.Validator.equalCheck;

import software.wings.beans.EcrConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.ecr.EcrClassicService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.EcrClassicBuildService;
import software.wings.utils.ArtifactType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by brett on 7/16/17.
 */
@Singleton
public class EcrClassicBuildServiceImpl implements EcrClassicBuildService {
  @Inject private EcrClassicService ecrClassicService;

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      EcrConfig ecrConfig, List<EncryptedDataDetail> encryptionDetails) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ECR.name());
    List<BuildDetails> builds =
        ecrClassicService.getBuilds(ecrConfig, encryptionDetails, artifactStreamAttributes.getImageName(), 50);
    return builds;
  }

  @Override
  public List<JobDetails> getJobs(
      EcrConfig ecrConfig, List<EncryptedDataDetail> encryptionDetails, Optional<String> parentJobName) {
    List<String> strings = ecrClassicService.listEcrRegistry(ecrConfig, encryptionDetails);
    return wrapJobNameWithJobDetails(strings);
  }

  @Override
  public List<String> getArtifactPaths(
      String jobName, String groupId, EcrConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Operation not supported by Docker Artifact Stream");
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      EcrConfig ecrConfig, List<EncryptedDataDetail> encryptionDetails) {
    throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Operation not supported by Docker Artifact Stream");
  }

  @Override
  public Map<String, String> getPlans(EcrConfig config, List<EncryptedDataDetail> encryptionDetails) {
    return getJobs(config, encryptionDetails, Optional.empty())
        .stream()
        .collect(Collectors.toMap(o -> o.getJobName(), o -> o.getJobName()));
  }

  @Override
  public Map<String, String> getPlans(
      EcrConfig config, List<EncryptedDataDetail> encryptionDetails, ArtifactType artifactType, String repositoryType) {
    return getPlans(config, encryptionDetails);
  }

  @Override
  public List<String> getGroupIds(String jobName, EcrConfig config, List<EncryptedDataDetail> encryptionDetails) {
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
    return ecrClassicService.validateCredentials(config, Collections.emptyList());
  }

  @Override
  public boolean validateArtifactSource(EcrConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) {
    return ecrClassicService.verifyRepository(config, encryptionDetails, artifactStreamAttributes.getImageName());
  }
}
