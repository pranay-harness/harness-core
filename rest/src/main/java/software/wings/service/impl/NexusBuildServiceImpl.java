package software.wings.service.impl;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import software.wings.beans.BambooConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
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
    return new ArrayList<>();
  }

  @Override
  public List<String> getJobs(NexusConfig config) {
    return Lists.newArrayList(nexusService.getRepositories(config).keySet());
  }

  @Override
  public List<String> getArtifactPaths(String jobName, NexusConfig config) {
    // Here Jobname as repo Id
    return nexusService.getArtifactPaths(config, jobName);
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, NexusConfig config) {
    return null;
  }
}
