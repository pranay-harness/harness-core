package software.wings.service.impl;

import static software.wings.utils.Validator.equalCheck;

import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.helpers.ext.artifactory.ArtifactoryService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.ArtifactoryBuildService;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by sgurubelli on 6/28/17.
 */
@Singleton
public class ArtifactoryBuildServiceImpl implements ArtifactoryBuildService {
  @Inject private ArtifactoryService artifactoryService;

  @Override
  public List<BuildDetails> getBuilds(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, ArtifactoryConfig artifactoryConfig) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.ARTIFACTORY.name());
    List<BuildDetails> builds = artifactoryService.getBuilds(
        artifactoryConfig, artifactStreamAttributes.getJobName(), artifactStreamAttributes.getImageName(), 50);
    return builds;
  }

  @Override
  public List<String> getJobs(ArtifactoryConfig jenkinsConfig) {
    return null;
  }

  @Override
  public List<String> getArtifactPaths(String jobName, String groupId, ArtifactoryConfig config) {
    return null;
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, ArtifactoryConfig config) {
    return null;
  }

  @Override
  public Map<String, String> getPlans(ArtifactoryConfig config) {
    return artifactoryService.getRepositories(config);
  }

  @Override
  public List<String> getGroupIds(String repoType, ArtifactoryConfig config) {
    return artifactoryService.getRepoPaths(config, repoType);
  }

  @Override
  public boolean validateArtifactServer(ArtifactoryConfig config) {
    return false;
  }

  @Override
  public boolean validateArtifactSource(ArtifactoryConfig config, ArtifactStreamAttributes artifactStreamAttributes) {
    return false;
  }
}
