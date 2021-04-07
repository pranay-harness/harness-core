package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessModule._930_DELEGATE_TASKS;
import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.network.Http.connectableHttpUrl;
import static io.harness.validation.Validator.equalCheck;

import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.impl.artifact.ArtifactServiceImpl.ARTIFACT_RETENTION_SIZE;
import static software.wings.utils.ArtifactType.DOCKER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidArtifactServerException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.helpers.ext.artifactory.ArtifactoryService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.service.intfc.ArtifactoryBuildService;
import software.wings.utils.ArtifactType;
import software.wings.utils.RepositoryType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.NotSupportedException;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by sgurubelli on 6/28/17.
 */
@TargetModule(_930_DELEGATE_TASKS)
@OwnedBy(CDC)
@Singleton
@Slf4j
public class ArtifactoryBuildServiceImpl implements ArtifactoryBuildService {
  public static final int MANUAL_PULL_ARTIFACTORY_LIMIT = 1000;
  @Inject private ArtifactoryService artifactoryService;

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.ARTIFACTORY.name());
    log.info("[Artifactory Delegate Selection] Get Builds for artifact stream {} and delegate selectors - {}",
        artifactStreamAttributes.getArtifactStreamId(), artifactoryConfig.getDelegateSelectors());
    return wrapNewBuildsWithLabels(
        getBuilds(appId, artifactStreamAttributes, artifactoryConfig, encryptionDetails, MANUAL_PULL_ARTIFACTORY_LIMIT),
        artifactStreamAttributes, artifactoryConfig);
  }

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails, int limit) {
    log.info("[Artifactory Delegate Selection] Get Builds for artifact stream {} and delegate selectors - {}",
        artifactStreamAttributes.getArtifactStreamId(), artifactoryConfig.getDelegateSelectors());
    return wrapNewBuildsWithLabels(
        getBuildsInternal(appId, artifactStreamAttributes, artifactoryConfig, encryptionDetails, limit),
        artifactStreamAttributes, artifactoryConfig);
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
    log.info("[Artifactory Delegate Selection] Get artifact paths for job name {} and delegate selectors - {}", jobName,
        config.getDelegateSelectors());
    if (isEmpty(groupId)) {
      log.info("Retrieving {} repo paths.", jobName);
      List<String> repoPaths = artifactoryService.getRepoPaths(config, encryptionDetails, jobName);
      log.info("Retrieved {} repo paths.", repoPaths.size());
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
    log.info("[Artifactory Delegate Selection] Get plans delegate selectors - {}", config.getDelegateSelectors());
    return artifactoryService.getRepositories(config, encryptionDetails);
  }

  @Override
  public Map<String, String> getPlans(ArtifactoryConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactType artifactType, String repositoryType) {
    log.info(
        "[Artifactory Delegate Selection] Get plans for artifactType {} repository type {} delegate selectors - {}",
        artifactType, repositoryType, config.getDelegateSelectors());
    if (RepositoryType.docker.name().equalsIgnoreCase(repositoryType) || artifactType == DOCKER) {
      return artifactoryService.getRepositories(config, encryptionDetails, DOCKER);
    }
    return artifactoryService.getRepositories(config, encryptionDetails, repositoryType);
  }

  @Override
  public Map<String, String> getPlans(
      ArtifactoryConfig config, List<EncryptedDataDetail> encryptionDetails, RepositoryType repositoryType) {
    log.info("[Artifactory Delegate Selection] Get plans for repository type {} delegate selectors - {}",
        repositoryType, config.getDelegateSelectors());
    return artifactoryService.getRepositories(config, encryptionDetails, repositoryType);
  }

  @Override
  public List<String> getGroupIds(
      String repoType, ArtifactoryConfig config, List<EncryptedDataDetail> encryptionDetails) {
    log.info("[Artifactory Delegate Selection] Get group ids for repoType {} delegate selectors - {}", repoType,
        config.getDelegateSelectors());
    log.info("Retrieving {} docker images.", repoType);
    List<String> repoPaths = artifactoryService.getRepoPaths(config, encryptionDetails, repoType);
    log.info("Retrieved {} docker images.", repoPaths.size());
    return repoPaths;
  }

  @Override
  public List<String> getGroupIds(String repositoryName, String repositoryType, ArtifactoryConfig config,
      List<EncryptedDataDetail> encryptionDetails) {
    log.info("[Artifactory Delegate Selection] Get groupIds for repositoryName {} delegate selectors - {}",
        repositoryName, config.getDelegateSelectors());
    log.info("Retrieving {} docker images.", repositoryName);
    List<String> repoPaths = artifactoryService.getRepoPaths(config, encryptionDetails, repositoryName);
    log.info("Retrieved {} docker images.", repoPaths.size());
    return repoPaths;
  }

  @Override
  public boolean validateArtifactServer(ArtifactoryConfig config, List<EncryptedDataDetail> encryptedDataDetails) {
    log.info("[Artifactory Delegate Selection] Validate artifact server delegate selectors - {}",
        config.getDelegateSelectors());
    if (!connectableHttpUrl(config.getArtifactoryUrl())) {
      throw new InvalidArtifactServerException(
          "Could not reach Artifactory Server at : " + config.getArtifactoryUrl(), USER);
    }
    return artifactoryService.isRunning(config, encryptedDataDetails);
  }

  @Override
  public boolean validateArtifactSource(ArtifactoryConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) {
    log.info("[Artifactory Delegate Selection] Validate artifact server job name {} delegate selectors - {}",
        artifactStreamAttributes.getJobName(), config.getDelegateSelectors());
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
