package software.wings.service.impl;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import software.wings.beans.AwsConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.amazons3.AmazonS3Service;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.AmazonS3BuildService;
import software.wings.utils.ArtifactType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author rktummala  on 7/30/17.
 */
@Singleton
public class AmazonS3BuildServiceImpl implements AmazonS3BuildService {
  @Inject private AmazonS3Service amazonS3Service;

  @Override
  public Map<String, String> getPlans(AwsConfig config, List<EncryptedDataDetail> encryptionDetails) {
    return amazonS3Service.getBuckets(config, encryptionDetails);
  }

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    String artifactName = artifactStreamAttributes.getArtifactName();
    return amazonS3Service.getArtifactsBuildDetails(awsConfig, encryptionDetails, artifactStreamAttributes.getJobName(),
        Lists.newArrayList(artifactName), artifactName.contains("*"));
  }

  @Override
  public List<JobDetails> getJobs(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, Optional<String> parentJobName) {
    return null;
  }

  @Override
  public List<String> getArtifactPaths(
      String bucketName, String groupId, AwsConfig config, List<EncryptedDataDetail> encryptionDetails) {
    return amazonS3Service.getArtifactPaths(config, encryptionDetails, bucketName);
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      AwsConfig config, List<EncryptedDataDetail> encryptionDetails) {
    return null;
  }

  @Override
  public Map<String, String> getPlans(
      AwsConfig config, List<EncryptedDataDetail> encryptionDetails, ArtifactType artifactType, String repositoryType) {
    return null;
  }

  @Override
  public List<String> getGroupIds(String repoType, AwsConfig config, List<EncryptedDataDetail> encryptionDetails) {
    return null;
  }

  @Override
  public boolean validateArtifactServer(AwsConfig config) {
    return false;
  }

  @Override
  public boolean validateArtifactSource(AwsConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) {
    return false;
  }

  @Override
  public Map<String, String> getBuckets(
      AwsConfig config, String projectId, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by Amazon S3 Build Service");
  }
}
