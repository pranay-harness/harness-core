package software.wings.service.impl;

import static io.harness.exception.WingsException.USER;
import static io.harness.network.Http.connectableHttpUrl;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.utils.Validator.equalCheck;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.config.NexusConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.helpers.ext.nexus.NexusService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.NexusBuildService;
import software.wings.utils.ArtifactType;
import software.wings.utils.RepositoryFormat;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by srinivas on 3/31/17.
 */
@Singleton
public class NexusBuildServiceImpl implements NexusBuildService {
  @Inject private NexusService nexusService;

  @Override
  public Map<String, String> getPlans(NexusConfig config, List<EncryptedDataDetail> encryptionDetails) {
    return nexusService.getRepositories(config, encryptionDetails);
  }

  @Override
  public Map<String, String> getPlans(NexusConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactType artifactType, String repositoryFormat) {
    if (!artifactType.equals(ArtifactType.DOCKER) && repositoryFormat != null
        && repositoryFormat.equals(RepositoryFormat.docker.name())) {
      throw new WingsException(format("Not supported for Artifact Type %s", artifactType), USER);
    }
    if (artifactType.equals(ArtifactType.DOCKER)) {
      return nexusService.getRepositories(config, encryptionDetails, RepositoryFormat.docker.name());
    }
    return nexusService.getRepositories(config, encryptionDetails, repositoryFormat);
  }

  @Override
  public Map<String, String> getPlans(
      NexusConfig config, List<EncryptedDataDetail> encryptionDetails, RepositoryFormat repositoryFormat) {
    return nexusService.getRepositories(config, encryptionDetails, repositoryFormat.name());
  }

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      NexusConfig config, List<EncryptedDataDetail> encryptionDetails) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.NEXUS.name());
    if (!appId.equals(GLOBAL_APP_ID)) {
      if (artifactStreamAttributes.getArtifactType() != null
          && artifactStreamAttributes.getArtifactType().equals(ArtifactType.DOCKER)) {
        return nexusService.getBuilds(config, encryptionDetails, artifactStreamAttributes, 50);
      } else if (artifactStreamAttributes.getRepositoryFormat().equals(RepositoryFormat.nuget.name())
          || artifactStreamAttributes.getRepositoryFormat().equals(RepositoryFormat.npm.name())) {
        return nexusService.getVersions(artifactStreamAttributes.getRepositoryFormat(), config, encryptionDetails,
            artifactStreamAttributes.getJobName(), artifactStreamAttributes.getNexusPackageName());
      } else {
        return nexusService.getVersions(config, encryptionDetails, artifactStreamAttributes.getJobName(),
            artifactStreamAttributes.getGroupId(), artifactStreamAttributes.getArtifactName());
      }
    } else {
      if (artifactStreamAttributes.getRepositoryFormat().equals(RepositoryFormat.docker.name())) {
        return nexusService.getBuilds(config, encryptionDetails, artifactStreamAttributes, 50);
      } else if (artifactStreamAttributes.getRepositoryFormat().equals(RepositoryFormat.nuget.name())
          || artifactStreamAttributes.getRepositoryFormat().equals(RepositoryFormat.npm.name())) {
        return nexusService.getVersions(artifactStreamAttributes.getRepositoryFormat(), config, encryptionDetails,
            artifactStreamAttributes.getJobName(), artifactStreamAttributes.getNexusPackageName());
      } else {
        return nexusService.getVersions(config, encryptionDetails, artifactStreamAttributes.getJobName(),
            artifactStreamAttributes.getGroupId(), artifactStreamAttributes.getArtifactName());
      }
    }
  }

  @Override
  public List<JobDetails> getJobs(
      NexusConfig config, List<EncryptedDataDetail> encryptionDetails, Optional<String> parentJobName) {
    List<String> jobNames = Lists.newArrayList(nexusService.getRepositories(config, encryptionDetails).keySet());
    return wrapJobNameWithJobDetails(jobNames);
  }

  @Override
  public List<String> getArtifactPaths(
      String repoId, String groupId, NexusConfig config, List<EncryptedDataDetail> encryptionDetails) {
    if (isBlank(groupId)) {
      return nexusService.getArtifactPaths(config, encryptionDetails, repoId);
    }
    return nexusService.getArtifactNames(config, encryptionDetails, repoId, groupId);
  }

  @Override
  public List<String> getGroupIds(
      String repositoryName, NexusConfig config, List<EncryptedDataDetail> encryptionDetails) {
    return nexusService.getGroupIdPaths(config, encryptionDetails, repositoryName, null);
  }

  @Override
  public List<String> getGroupIds(
      String repositoryName, String repositoryFormat, NexusConfig config, List<EncryptedDataDetail> encryptionDetails) {
    return nexusService.getGroupIdPaths(config, encryptionDetails, repositoryName, repositoryFormat);
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      NexusConfig config, List<EncryptedDataDetail> encryptionDetails) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.NEXUS.name());
    return nexusService.getLatestVersion(config, encryptionDetails, artifactStreamAttributes.getJobName(),
        artifactStreamAttributes.getGroupId(), artifactStreamAttributes.getArtifactName());
  }

  @Override
  public boolean validateArtifactServer(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    if (!connectableHttpUrl(nexusConfig.getNexusUrl())) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, USER)
          .addParam("message", "Could not reach Nexus Server at : " + nexusConfig.getNexusUrl());
    }
    return nexusService.isRunning(nexusConfig, encryptedDataDetails);
  }

  @Override
  public boolean validateArtifactSource(NexusConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) {
    return true;
  }

  @Override
  public Map<String, String> getBuckets(
      NexusConfig config, String projectId, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by Nexus Artifact Stream");
  }

  @Override
  public List<String> getSmbPaths(NexusConfig config, List<EncryptedDataDetail> encryptionDetails) {
    throw new InvalidRequestException("Operation not supported by Nexus Build Service", WingsException.USER);
  }

  @Override
  public List<String> getArtifactPathsByStreamType(
      NexusConfig config, List<EncryptedDataDetail> encryptionDetails, String streamType) {
    throw new InvalidRequestException("Operation not supported by Nexus Build Service", WingsException.USER);
  }
}
