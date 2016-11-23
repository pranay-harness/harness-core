package software.wings.service.impl;

import software.wings.beans.BambooConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BambooBuildService;
import software.wings.service.intfc.SettingsService;

import java.util.List;
import java.util.Set;
import javax.inject.Inject;

/**
 * Created by anubhaw on 11/22/16.
 */
public class BambooBuildServiceImpl implements BambooBuildService {
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private SettingsService settingsService;

  @Override
  public List<BuildDetails> getBuilds(String appId, String artifactSourceId, BambooConfig bambooConfig) {
    return null;
  }

  @Override
  public Set<String> getJobs(BambooConfig bambooConfig) {
    return null;
  }

  @Override
  public Set<String> getArtifactPaths(String jobName, BambooConfig bambooConfig) {
    return null;
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, String artifactStreamId, BambooConfig bambooConfig) {
    return null;
  }
}
