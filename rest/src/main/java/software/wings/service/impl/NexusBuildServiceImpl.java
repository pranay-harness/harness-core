package software.wings.service.impl;

import static software.wings.utils.Validator.equalCheck;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.lang.StringUtils;
import software.wings.beans.BambooConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.config.NexusConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.nexus.NexusService;
import software.wings.service.intfc.NexusBuildService;

/**
 * Created by srinivas on 3/31/17.
 */
@Singleton
public class NexusBuildServiceImpl implements NexusBuildService {
  @Inject private NexusService nexusService;

  @Override
  public Map<String, String> getPlans(NexusConfig config) {
    return nexusService.getRepositories(config);
  }

  @Override
  public List<BuildDetails> getBuilds(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, NexusConfig config) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.NEXUS.name());
    return nexusService.getVersions(config, artifactStreamAttributes.getJobName(),
        artifactStreamAttributes.getGroupId(), artifactStreamAttributes.getArtifactName());
  }

  @Override
  public List<String> getJobs(NexusConfig config) {
    return Lists.newArrayList(nexusService.getRepositories(config).keySet());
  }

  @Override
  public List<String> getArtifactPaths(String repoId, String groupId, NexusConfig config) {
    if (StringUtils.isBlank(groupId)) {
      return nexusService.getArtifactPaths(config, repoId);
    }
    return nexusService.getArtifactNames(config, repoId, groupId);
  }

  @Override
  public List<String> getGroupIds(String repoType, NexusConfig config) {
    return nexusService.getGroupIdPaths(config, repoType);
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, NexusConfig config) {
    equalCheck(artifactStreamAttributes.getArtifactStreamType(), ArtifactStreamType.NEXUS.name());
    return nexusService.getLatestVersion(config, artifactStreamAttributes.getJobName(),
        artifactStreamAttributes.getGroupId(), artifactStreamAttributes.getArtifactName());
  }

  @Override
  public void validateArtifactServer(NexusConfig config) {}
  @Override
  public void validateArtifactSource(NexusConfig config, ArtifactStreamAttributes artifactStreamAttributes) {}
}
