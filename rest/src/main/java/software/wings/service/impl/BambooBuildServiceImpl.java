package software.wings.service.impl;

import static io.harness.network.Http.connectableHttpUrl;
import static software.wings.exception.WingsException.USER;
import static software.wings.utils.Validator.equalCheck;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.BambooConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.BambooBuildService;
import software.wings.utils.ArtifactType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by anubhaw on 11/22/16.
 */
@Singleton
public class BambooBuildServiceImpl implements BambooBuildService {
  @Inject private BambooService bambooService;

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.BAMBOO.name());
    return bambooService.getBuilds(bambooConfig, encryptionDetails, artifactStreamAttributes.getJobName(), 50);
  }

  @Override
  public List<JobDetails> getJobs(
      BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails, Optional<String> parentJobName) {
    List<String> strings = Lists.newArrayList(bambooService.getPlanKeys(bambooConfig, encryptionDetails).keySet());
    return wrapJobNameWithJobDetails(strings);
  }

  @Override
  public Map<String, String> getPlans(BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails) {
    return bambooService.getPlanKeys(bambooConfig, encryptionDetails);
  }

  @Override
  public Map<String, String> getPlans(BambooConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactType artifactType, String repositoryType) {
    return getPlans(config, encryptionDetails);
  }

  @Override
  public List<String> getArtifactPaths(
      String jobName, String groupId, BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails) {
    return bambooService.getArtifactPath(bambooConfig, encryptionDetails, jobName);
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.BAMBOO.name());

    return bambooService.getLastSuccessfulBuild(bambooConfig, encryptionDetails, artifactStreamAttributes.getJobName());
  }

  @Override
  public List<String> getGroupIds(
      String jobName, BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by Bamboo Artifact Stream", USER);
  }

  @Override
  public boolean validateArtifactServer(BambooConfig bambooConfig) {
    if (!connectableHttpUrl(bambooConfig.getBambooUrl())) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, USER)
          .addParam("message", "Could not reach Bamboo Server at : " + bambooConfig.getBambooUrl());
    }
    // check for credentials
    return bambooService.isRunning(bambooConfig, Collections.emptyList());
  }

  @Override
  public boolean validateArtifactSource(BambooConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) {
    return true;
  }

  @Override
  public Map<String, String> getBuckets(
      BambooConfig bambooConfig, String projectId, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by Bamboo Artifact Stream");
  }
}
