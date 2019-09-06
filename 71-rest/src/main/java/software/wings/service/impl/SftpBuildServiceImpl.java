package software.wings.service.impl;

import static io.harness.exception.WingsException.USER;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SftpConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.helpers.ext.sftp.SftpService;
import software.wings.service.intfc.SftpBuildService;
import software.wings.utils.ArtifactType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
@Slf4j
public class SftpBuildServiceImpl implements SftpBuildService {
  @Inject private SftpService sftpService;
  @Inject private SftpHelperService sftpHelperService;

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      SftpConfig sftpConfig, List<EncryptedDataDetail> encryptionDetails) {
    String artifactName = artifactStreamAttributes.getArtifactName();
    return wrapNewBuildsWithLabels(sftpService.getBuildDetails(sftpConfig, encryptionDetails,
                                       Lists.newArrayList(artifactName), artifactName.contains("*")),
        artifactStreamAttributes, sftpConfig, encryptionDetails);
  }

  @Override
  public List<String> getArtifactPaths(
      String jobName, String groupId, SftpConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by SFTP Build Service", USER);
  }

  @Override
  public boolean validateArtifactServer(SftpConfig config, List<EncryptedDataDetail> encryptedDataDetails) {
    String sftpConnectionHost = sftpHelperService.getSFTPConnectionHost(config.getSftpUrl());
    if (!sftpHelperService.isConnectibleSFTPServer(sftpConnectionHost)) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, USER)
          .addParam("message", "Could not reach SFTP Server at : " + config.getSftpUrl());
    }
    return sftpService.isRunning(config, encryptedDataDetails);
  }

  @Override
  public List<JobDetails> getJobs(
      SftpConfig sftpConfig, List<EncryptedDataDetail> encryptionDetails, Optional<String> parentJobName) {
    throw new InvalidRequestException("Operation not supported by SFTP Build Service", USER);
  }

  @Override
  public List<String> getArtifactPathsByStreamType(
      SftpConfig config, List<EncryptedDataDetail> encryptionDetails, String streamType) {
    if (streamType.equals(ArtifactStreamType.SFTP.name())) {
      List<String> artifactPaths = sftpService.getArtifactPaths(config, encryptionDetails);
      logger.info("Retrieved {} artifact paths from SFTP server.", artifactPaths.size());
      return artifactPaths;
    } else {
      throw new InvalidRequestException("Invalid Artifact Stream type received for getArtifactPaths.", USER);
    }
  }

  @Override
  public List<String> getSmbPaths(SftpConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by SFTP Build Service", USER);
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      SftpConfig sftpConfig, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by SFTP Build Service", USER);
  }

  @Override
  public Map<String, String> getPlans(SftpConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by SFTP Artifact Stream");
  }

  @Override
  public Map<String, String> getBuckets(
      SftpConfig sftpConfig, String projectId, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by SFTP Artifact Server");
  }

  @Override
  public Map<String, String> getPlans(SftpConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactType artifactType, String repositoryType) {
    throw new InvalidRequestException("Operation not supported by SFTP Artifact Stream");
  }

  @Override
  public List<String> getGroupIds(String repoType, SftpConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by SFTP Artifact Stream");
  }

  @Override
  public boolean validateArtifactSource(SftpConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) {
    throw new InvalidRequestException("Operation not supported by SFTP Artifact Stream");
  }
}