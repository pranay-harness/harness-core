package software.wings.service.impl;

import static software.wings.utils.HttpUtil.connectableHttpUrl;
import static software.wings.utils.HttpUtil.validUrl;
import static software.wings.utils.Validator.equalCheck;

import com.google.common.collect.Lists;

import org.apache.commons.lang.StringUtils;
import software.wings.beans.ErrorCode;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.config.NexusConfig;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.helpers.ext.nexus.NexusService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.NexusBuildService;
import software.wings.utils.ArtifactType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

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
      ArtifactType artifactType, String repositoryType) {
    return nexusService.getRepositories(config, encryptionDetails, artifactType);
  }

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      NexusConfig config, List<EncryptedDataDetail> encryptionDetails) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.NEXUS.name());
    if (artifactStreamAttributes.getArtifactType().equals(ArtifactType.DOCKER)) {
      return nexusService.getBuilds(config, encryptionDetails, artifactStreamAttributes.getJobName(),
          artifactStreamAttributes.getImageName(), 50);
    } else {
      return nexusService.getVersions(config, encryptionDetails, artifactStreamAttributes.getJobName(),
          artifactStreamAttributes.getGroupId(), artifactStreamAttributes.getArtifactName());
    }
  }

  @Override
  public List<JobDetails> getJobs(
      NexusConfig config, List<EncryptedDataDetail> encryptionDetails, Optional<String> parentJobName) {
    ArrayList<String> jobNames = Lists.newArrayList(nexusService.getRepositories(config, encryptionDetails).keySet());
    return wrapJobNameWithJobDetails(jobNames);
  }

  @Override
  public List<String> getArtifactPaths(
      String repoId, String groupId, NexusConfig config, List<EncryptedDataDetail> encryptionDetails) {
    if (StringUtils.isBlank(groupId)) {
      return nexusService.getArtifactPaths(config, encryptionDetails, repoId);
    }
    return nexusService.getArtifactNames(config, encryptionDetails, repoId, groupId);
  }

  @Override
  public List<String> getGroupIds(String repoType, NexusConfig config, List<EncryptedDataDetail> encryptionDetails) {
    return nexusService.getGroupIdPaths(config, encryptionDetails, repoType);
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      NexusConfig config, List<EncryptedDataDetail> encryptionDetails) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.NEXUS.name());
    return nexusService.getLatestVersion(config, encryptionDetails, artifactStreamAttributes.getJobName(),
        artifactStreamAttributes.getGroupId(), artifactStreamAttributes.getArtifactName());
  }

  @Override
  public boolean validateArtifactServer(NexusConfig config) {
    if (!validUrl(config.getNexusUrl())) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Nexus URL must be a valid URL");
    }
    if (!connectableHttpUrl(config.getNexusUrl())) {
      throw new WingsException(
          ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Could not reach Nexus Server at : " + config.getNexusUrl());
    }
    return nexusService.getRepositories(config, Collections.emptyList()) != null;
  }

  @Override
  public boolean validateArtifactSource(NexusConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes) {
    return true;
  }
}
