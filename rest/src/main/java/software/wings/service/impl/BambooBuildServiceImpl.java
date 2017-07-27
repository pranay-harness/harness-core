package software.wings.service.impl;

import static software.wings.utils.HttpUtil.connectableHttpUrl;
import static software.wings.utils.HttpUtil.validUrl;
import static software.wings.utils.Validator.equalCheck;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import software.wings.beans.BambooConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.service.intfc.BambooBuildService;
import software.wings.utils.ArtifactType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;

/**
 * Created by anubhaw on 11/22/16.
 */
@Singleton
public class BambooBuildServiceImpl implements BambooBuildService {
  @Inject private BambooService bambooService;

  @Override
  public List<BuildDetails> getBuilds(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, BambooConfig bambooConfig) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.BAMBOO.name());

    return bambooService.getBuilds(bambooConfig, artifactStreamAttributes.getJobName(), 50);
  }

  @Override
  public List<JobDetails> getJobs(BambooConfig bambooConfig, Optional<String> parentJobName) {
    ArrayList<String> strings = Lists.newArrayList(bambooService.getPlanKeys(bambooConfig).keySet());
    return wrapJobNameWithJobDetails(strings);
  }

  @Override
  public Map<String, String> getPlans(BambooConfig bambooConfig) {
    return bambooService.getPlanKeys(bambooConfig);
  }

  @Override
  public Map<String, String> getPlans(BambooConfig config, ArtifactType artifactType) {
    return getPlans(config);
  }

  @Override
  public List<String> getArtifactPaths(String jobName, String groupId, BambooConfig bambooConfig) {
    return bambooService.getArtifactPath(bambooConfig, jobName);
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, BambooConfig bambooConfig) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.BAMBOO.name());

    return bambooService.getLastSuccessfulBuild(bambooConfig, artifactStreamAttributes.getJobName());
  }

  @Override
  public List<String> getGroupIds(String jobName, BambooConfig bambooConfig) {
    throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Operation not supported by Bamboo Artifact Stream");
  }

  @Override
  public boolean validateArtifactServer(BambooConfig bambooConfig) {
    if (!validUrl(bambooConfig.getBambooUrl())) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Bamboo URL must be a valid URL");
    }

    if (!connectableHttpUrl(bambooConfig.getBambooUrl())) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message",
          "Could not reach Bamboo Server at : " + bambooConfig.getBambooUrl());
    }
    // check for credentials
    return bambooService.isRunning(bambooConfig);
  }

  @Override
  public boolean validateArtifactSource(BambooConfig config, ArtifactStreamAttributes artifactStreamAttributes) {
    return true;
  }
}
