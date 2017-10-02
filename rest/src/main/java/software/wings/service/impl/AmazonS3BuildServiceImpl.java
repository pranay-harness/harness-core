package software.wings.service.impl;

import software.wings.beans.AwsConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.amazons3.AmazonS3Service;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.service.intfc.AmazonS3BuildService;
import software.wings.utils.ArtifactType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author rktummala  on 7/30/17.
 */
@Singleton
public class AmazonS3BuildServiceImpl implements AmazonS3BuildService {
  @Inject private AmazonS3Service amazonS3Service;

  @Override
  public Map<String, String> getPlans(AwsConfig config) {
    return amazonS3Service.getBuckets(config);
  }

  @Override
  public List<BuildDetails> getBuilds(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, AwsConfig config) {
    return null;
  }

  @Override
  public List<JobDetails> getJobs(AwsConfig jenkinsConfig, Optional<String> parentJobName) {
    return null;
  }

  @Override
  public List<String> getArtifactPaths(String bucketName, String groupId, AwsConfig config) {
    return amazonS3Service.getArtifactPaths(config, bucketName);
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, AwsConfig config) {
    return amazonS3Service.getArtifactMetadata(config, artifactStreamAttributes, appId);
  }

  @Override
  public Map<String, String> getPlans(AwsConfig config, ArtifactType artifactType) {
    return null;
  }

  @Override
  public List<String> getGroupIds(String repoType, AwsConfig config) {
    return null;
  }

  @Override
  public boolean validateArtifactServer(AwsConfig config) {
    return false;
  }

  @Override
  public boolean validateArtifactSource(AwsConfig config, ArtifactStreamAttributes artifactStreamAttributes) {
    return false;
  }
}
