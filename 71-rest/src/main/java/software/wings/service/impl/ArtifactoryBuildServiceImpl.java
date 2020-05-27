package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.network.Http.connectableHttpUrl;
import static io.harness.validation.Validator.equalCheck;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.impl.artifact.ArtifactServiceImpl.ARTIFACT_RETENTION_SIZE;
import static software.wings.utils.ArtifactType.DOCKER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.helpers.ext.artifactory.ArtifactoryService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.service.intfc.ArtifactoryBuildService;
import software.wings.utils.ArtifactType;
import software.wings.utils.RepositoryType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.NotSupportedException;

/**
 * Created by sgurubelli on 6/28/17.
 */
@OwnedBy(CDC)
@Singleton
@Slf4j
public class ArtifactoryBuildServiceImpl implements ArtifactoryBuildService {
  @Inject private ArtifactoryService artifactoryService;

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.ARTIFACTORY.name());
    return wrapNewBuildsWithLabels(
        getBuilds(appId, artifactStreamAttributes, artifactoryConfig, encryptionDetails, ARTIFACT_RETENTION_SIZE),
        artifactStreamAttributes, artifactoryConfig, encryptionDetails);
  }

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails, int limit) {
    return wrapNewBuildsWithLabels(
        getBuildsInternal(appId, artifactStreamAttributes, artifactoryConfig, encryptionDetails, limit),
        artifactStreamAttributes, artifactoryConfig, encryptionDetails);
  }

  private List<BuildDetails> getBuildsInternal(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails, int limit) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.ARTIFACTORY.name());
    if (!appId.equals(GLOBAL_APP_ID)) {
      if (artifactStreamAttributes.getArtifactType() == DOCKER
          || RepositoryType.docker.name().equalsIgnoreCase(artifactStreamAttributes.getRepositoryType())) {
        return artifactoryService.getBuilds(
            artifactoryConfig, encryptionDetails, artifactStreamAttributes, limit == -1 ? 1000 : limit);
      } else {
        return artifactoryService.getFilePaths(artifactoryConfig, encryptionDetails,
            artifactStreamAttributes.getJobName(), artifactStreamAttributes.getArtifactPattern(),
            artifactStreamAttributes.getRepositoryType(), limit == -1 ? ARTIFACT_RETENTION_SIZE : limit);
      }
    } else {
      if (artifactStreamAttributes.getRepositoryType().equals(RepositoryType.docker.name())) {
        return artifactoryService.getBuilds(
            artifactoryConfig, encryptionDetails, artifactStreamAttributes, limit == -1 ? 1000 : limit);
      } else {
        return artifactoryService.getFilePaths(artifactoryConfig, encryptionDetails,
            artifactStreamAttributes.getJobName(), artifactStreamAttributes.getArtifactPattern(),
            artifactStreamAttributes.getRepositoryType(), limit == -1 ? ARTIFACT_RETENTION_SIZE : limit);
      }
    }
  }

  @Override
  public List<JobDetails> getJobs(
      ArtifactoryConfig config, List<EncryptedDataDetail> encryptionDetails, Optional<String> parentJobName) {
    throw new NotSupportedException();
  }

  @Override
  public List<String> getArtifactPaths(
      String jobName, String groupId, ArtifactoryConfig config, List<EncryptedDataDetail> encryptionDetails) {
    if (isEmpty(groupId)) {
      logger.info("Retrieving {} repo paths.", jobName);
      List<String> repoPaths = artifactoryService.getRepoPaths(config, encryptionDetails, jobName);
      logger.info("Retrieved {} repo paths.", repoPaths.size());
      return repoPaths;
    }
    return new ArrayList<>();
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails) {
    return null;
  }

  @Override
  public Map<String, String> getPlans(ArtifactoryConfig config, List<EncryptedDataDetail> encryptionDetails) {
    return artifactoryService.getRepositories(config, encryptionDetails);
  }

  @Override
  public Map<String, String> getPlans(ArtifactoryConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactType artifactType, String repositoryType) {
    if (RepositoryType.docker.name().equalsIgnoreCase(repositoryType) || artifactType == DOCKER) {
      return artifactoryService.getRepositories(config, encryptionDetails, DOCKER);
    }
    return artifactoryService.getRepositories(config, encryptionDetails, repositoryType);
  }

  @Override
  public Map<String, String> getPlans(
      ArtifactoryConfig config, List<EncryptedDataDetail> encryptionDetails, RepositoryType repositoryType) {
    return artifactoryService.getRepositories(config, encryptionDetails, repositoryType);
  }

  @Override
  public List<String> getGroupIds(
      String repoType, ArtifactoryConfig config, List<EncryptedDataDetail> encryptionDetails) {
    logger.info("Retrieving {} docker images.", repoType);
    List<String> repoPaths = artifactoryService.getRepoPaths(config, encryptionDetails, repoType);
    logger.info("Retrieved {} docker images.", repoPaths.size());
    return repoPaths;
  }

  @Override
  public List<String> getGroupIds(String repositoryName, String repositoryType, ArtifactoryConfig config,
      List<EncryptedDataDetail> encryptionDetails) {
    logger.info("Retrieving {} docker images.", repositoryName);
    List<String> repoPaths = artifactoryService.getRepoPaths(config, encryptionDetails, repositoryName);
    logger.info("Retrieved {} docker images.", repoPaths.size());
    return repoPaths;
  }

  @Override
  public boolean validateArtifactServer(ArtifactoryConfig config, List<EncryptedDataDetail> encryptedDataDetails) {
    if (!connectableHttpUrl(config.getArtifactoryUrl())) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, USER)
          .addParam("message", "Could not reach Artifactory Server at : " + config.getArtifactoryUrl());
    }
    return artifactoryService.isRunning(config, encryptedDataDetails);
  }

  @Override
  public boolean validateArtifactSource(ArtifactoryConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) {
    if (artifactStreamAttributes.getArtifactPattern() != null) {
      return artifactoryService.validateArtifactPath(config, encryptionDetails, artifactStreamAttributes.getJobName(),
          artifactStreamAttributes.getArtifactPattern(), artifactStreamAttributes.getRepositoryType());
    }
    return true;
  }

  @Override
  public Map<String, String> getBuckets(
      ArtifactoryConfig config, String projectId, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by Artifactory");
  }

  @Override
  public List<String> getSmbPaths(ArtifactoryConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by Artifactory Build Service", WingsException.USER);
  }

  @Override
  public List<String> getArtifactPathsByStreamType(
      ArtifactoryConfig config, List<EncryptedDataDetail> encryptionDetails, String streamType) {
    throw new InvalidRequestException("Operation not supported by Artifactory Build Service", WingsException.USER);
  }
}
