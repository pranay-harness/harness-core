package software.wings.service.impl;

import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.utils.HttpUtil.connectableHttpUrl;
import static software.wings.utils.HttpUtil.validUrl;
import static software.wings.utils.Validator.equalCheck;

import software.wings.beans.AwsConfig;
import software.wings.beans.EcrConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.ecr.EcrService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.service.intfc.EcrBuildService;
import software.wings.utils.ArtifactType;

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
public class EcrBuildServiceImpl implements EcrBuildService {
  @Inject private EcrService ecrService;

  @Override
  public List<BuildDetails> getBuilds(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, AwsConfig awsConfig) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ECR.name());
    List<BuildDetails> builds = ecrService.getBuilds(
        awsConfig, artifactStreamAttributes.getRegion(), artifactStreamAttributes.getImageName(), 50);
    return builds;
  }

  @Override
  public List<JobDetails> getJobs(AwsConfig awsConfig, Optional<String> parentJobName) {
    List<String> regions = ecrService.listRegions(awsConfig);
    return wrapJobNameWithJobDetails(regions);
  }

  @Override
  public List<String> getArtifactPaths(String region, String groupId, AwsConfig awsConfig) {
    return ecrService.listEcrRegistry(awsConfig, region);
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, AwsConfig awsConfig) {
    throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Operation not supported by ECR Artifact Stream");
  }

  @Override
  public Map<String, String> getPlans(AwsConfig config) {
    return getJobs(config, Optional.empty())
        .stream()
        .collect(Collectors.toMap(o -> o.getJobName(), o -> o.getJobName()));
  }

  @Override
  public Map<String, String> getPlans(AwsConfig config, ArtifactType artifactType) {
    return getPlans(config);
  }

  @Override
  public List<String> getGroupIds(String jobName, AwsConfig config) {
    throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Operation not supported by ECR Artifact Stream");
  }

  @Override
  public boolean validateArtifactServer(AwsConfig config) {
    throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Operation not supported by ECR Artifact Stream");
  }

  @Override
  public boolean validateArtifactSource(AwsConfig config, ArtifactStreamAttributes artifactStreamAttributes) {
    return ecrService.verifyRepository(
        config, artifactStreamAttributes.getRegion(), artifactStreamAttributes.getImageName());
  }
}
