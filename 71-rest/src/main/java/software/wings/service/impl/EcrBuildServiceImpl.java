package software.wings.service.impl;

import static io.harness.exception.WingsException.SRE;
import static io.harness.exception.WingsException.USER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.utils.Validator.equalCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import software.wings.beans.AwsConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.ecr.EcrService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.EcrBuildService;
import software.wings.utils.ArtifactType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by brett on 7/16/17.
 */
@Singleton
public class EcrBuildServiceImpl implements EcrBuildService {
  @Inject private EcrService ecrService;

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ECR.name());
    return ecrService.getBuilds(awsConfig, encryptionDetails, artifactStreamAttributes.getRegion(),
        artifactStreamAttributes.getImageName(), 50);
  }

  @Override
  public List<JobDetails> getJobs(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, Optional<String> parentJobName) {
    List<String> regions = ecrService.listRegions(awsConfig, encryptionDetails);
    return wrapJobNameWithJobDetails(regions);
  }

  @Override
  public List<String> getArtifactPaths(
      String region, String groupId, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    return ecrService.listEcrRegistry(awsConfig, encryptionDetails, region);
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by ECR Artifact Stream", SRE);
  }

  @Override
  public Map<String, String> getPlans(AwsConfig config, List<EncryptedDataDetail> encryptionDetails) {
    return getJobs(config, encryptionDetails, Optional.empty())
        .stream()
        .collect(Collectors.toMap(JobDetails::getJobName, JobDetails::getJobName));
  }

  @Override
  public Map<String, String> getPlans(
      AwsConfig config, List<EncryptedDataDetail> encryptionDetails, ArtifactType artifactType, String repositoryType) {
    return getPlans(config, encryptionDetails);
  }

  @Override
  public List<String> getGroupIds(String jobName, AwsConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by ECR Artifact Stream", USER);
  }

  @Override
  public boolean validateArtifactServer(AwsConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by ECR Artifact Stream", USER);
  }

  @Override
  public boolean validateArtifactSource(AwsConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) {
    return ecrService.verifyRepository(
        config, encryptionDetails, artifactStreamAttributes.getRegion(), artifactStreamAttributes.getImageName());
  }

  @Override
  public Map<String, String> getBuckets(
      AwsConfig config, String projectId, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by ECR Artifact Stream", USER);
  }
}
