package software.wings.service.impl;

import static software.wings.beans.ErrorCode.INVALID_ARTIFACT_SERVER;
import static software.wings.utils.ArtifactType.DOCKER;
import static software.wings.utils.HttpUtil.connectableHttpUrl;
import static software.wings.utils.HttpUtil.validUrl;
import static software.wings.utils.Validator.equalCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.artifactory.ArtifactoryService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.ArtifactoryBuildService;
import software.wings.utils.ArtifactType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.NotSupportedException;

/**
 * Created by sgurubelli on 6/28/17.
 */
@Singleton
public class ArtifactoryBuildServiceImpl implements ArtifactoryBuildService {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactoryBuildServiceImpl.class);

  @Inject private ArtifactoryService artifactoryService;

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.ARTIFACTORY.name());
    if (artifactStreamAttributes.getArtifactType().equals(DOCKER)) {
      return artifactoryService.getBuilds(artifactoryConfig, encryptionDetails, artifactStreamAttributes.getJobName(),
          artifactStreamAttributes.getImageName(), 50);
    } else {
      if (artifactStreamAttributes.isMetadataOnly()) {
        return artifactoryService.getFilePaths(artifactoryConfig, encryptionDetails,
            artifactStreamAttributes.getJobName(), artifactStreamAttributes.getArtifactPattern(),
            artifactStreamAttributes.getRepositoryType(), 25);
      } else {
        return artifactoryService.getFilePaths(artifactoryConfig, encryptionDetails,
            artifactStreamAttributes.getJobName(), artifactStreamAttributes.getArtifactPattern(),
            artifactStreamAttributes.getRepositoryType(), 25);
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
    if (StringUtils.isEmpty(groupId)) {
      logger.info("Retrieving {} repo paths.", jobName);
      List<String> repoPaths = artifactoryService.getRepoPaths(config, encryptionDetails, jobName);
      logger.info("Retrieved {} repo paths.", repoPaths.size());
      return repoPaths;
    } else {
      return artifactoryService.getArtifactIds(config, encryptionDetails, jobName, groupId);
    }
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails) {
    String[] artifactPaths = artifactStreamAttributes.getArtifactPattern().split("/");
    if (artifactPaths.length < 4) {
      throw new WingsException(INVALID_ARTIFACT_SERVER)
          .addParam("message", "Not in maven style format. Sample format: com/mycompany/myservice/.*/myservice*.war");
    }
    String groupId =
        getGroupId(Arrays.stream(artifactPaths).limit(artifactPaths.length - 3).collect(Collectors.toList()));
    String artifactId = artifactPaths[artifactPaths.length - 3];
    return artifactoryService.getLatestVersion(
        artifactoryConfig, encryptionDetails, artifactStreamAttributes.getJobName(), groupId, artifactId);
  }

  private String getGroupId(List<String> pathElems) {
    StringBuilder groupIdBuilder = new StringBuilder();
    for (int i = 0; i < pathElems.size(); i++) {
      groupIdBuilder.append(pathElems.get(i));
      if (i != pathElems.size() - 1) {
        groupIdBuilder.append(".");
      }
    }
    return groupIdBuilder.toString();
  }

  @Override
  public Map<String, String> getPlans(ArtifactoryConfig config, List<EncryptedDataDetail> encryptionDetails) {
    return artifactoryService.getRepositories(config, encryptionDetails);
  }

  @Override
  public Map<String, String> getPlans(ArtifactoryConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactType artifactType, String repositoryType) {
    if (artifactType.equals(DOCKER)) {
      return artifactoryService.getRepositories(config, encryptionDetails, artifactType);
    }
    return artifactoryService.getRepositories(config, encryptionDetails, repositoryType);
  }

  @Override
  public List<String> getGroupIds(
      String repoType, ArtifactoryConfig config, List<EncryptedDataDetail> encryptionDetails) {
    logger.info("Retrieving {} Group Ids.", repoType);
    List<String> repoPaths = artifactoryService.getRepoPaths(config, encryptionDetails, repoType);
    logger.info("Retrieved {} Group Ids.", repoPaths.size());
    return repoPaths;
  }

  @Override
  public boolean validateArtifactServer(ArtifactoryConfig config) {
    if (!validUrl(config.getArtifactoryUrl())) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER)
          .addParam("message", "Artifactory URL must be a valid URL");
    }
    if (!connectableHttpUrl(config.getArtifactoryUrl())) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER)
          .addParam("message", "Could not reach Artifactory Server at : " + config.getArtifactoryUrl());
    }
    return artifactoryService.getRepositories(config, Collections.emptyList()) != null;
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
}
