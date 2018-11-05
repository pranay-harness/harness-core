package software.wings.service.impl;

import static io.harness.exception.WingsException.USER;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SmbConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.helpers.ext.smb.SmbService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.SmbBuildService;
import software.wings.utils.ArtifactType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SmbBuildServiceImpl implements SmbBuildService {
  private static final Logger logger = LoggerFactory.getLogger(SmbBuildServiceImpl.class);

  @Inject private SmbService smbService;
  @Inject private SmbHelperService smbHelperService;

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      SmbConfig smbConfig, List<EncryptedDataDetail> encryptionDetails) {
    String artifactName = artifactStreamAttributes.getArtifactName();
    return smbService.getBuildDetails(
        smbConfig, encryptionDetails, Lists.newArrayList(artifactName), artifactName.contains("*"));
  }

  @Override
  public List<String> getArtifactPaths(
      String jobName, String groupId, SmbConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by SMB Build Service", USER);
  }

  @Override
  public boolean validateArtifactServer(SmbConfig config, List<EncryptedDataDetail> encryptedDataDetails) {
    String smbConnectionHost = smbHelperService.getSMBConnectionHost(config.getSmbUrl());
    if (!smbHelperService.isConnetableSMBServer(smbConnectionHost)) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, USER)
          .addParam("message", "Could not reach SMB Server at : " + config.getSmbUrl());
    }
    return smbService.isRunning(config, encryptedDataDetails);
  }

  @Override
  public List<JobDetails> getJobs(
      SmbConfig smbConfig, List<EncryptedDataDetail> encryptionDetails, Optional<String> parentJobName) {
    throw new InvalidRequestException("Operation not supported by SMB Build Service", USER);
  }

  @Override
  public List<String> getSmbPaths(SmbConfig config, List<EncryptedDataDetail> encryptionDetails) {
    List<String> artifactPaths = smbService.getArtifactPaths(config, encryptionDetails);
    logger.info("Retrieved {} artifact paths from SMB server.", artifactPaths.size());
    return artifactPaths;
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      SmbConfig smbConfig, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by SMB Build Service", USER);
  }

  @Override
  public Map<String, String> getPlans(SmbConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by SMB Artifact Stream");
  }

  @Override
  public Map<String, String> getBuckets(
      SmbConfig smbConfig, String projectId, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by SMB Artifact Server");
  }

  @Override
  public Map<String, String> getPlans(
      SmbConfig config, List<EncryptedDataDetail> encryptionDetails, ArtifactType artifactType, String repositoryType) {
    throw new InvalidRequestException("Operation not supported by SMB Artifact Stream");
  }

  @Override
  public List<String> getGroupIds(String repoType, SmbConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by SMB Artifact Stream");
  }

  @Override
  public boolean validateArtifactSource(SmbConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) {
    throw new InvalidRequestException("Operation not supported by SMB Artifact Stream");
  }
}
