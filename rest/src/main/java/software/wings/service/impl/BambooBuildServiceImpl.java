package software.wings.service.impl;

import static software.wings.utils.Validator.equalCheck;
import static software.wings.utils.Validator.notNullCheck;

import software.wings.beans.BambooConfig;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.SourceType;
import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BambooBuildService;
import software.wings.service.intfc.SettingsService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

/**
 * Created by anubhaw on 11/22/16.
 */
public class BambooBuildServiceImpl implements BambooBuildService {
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private SettingsService settingsService;
  @Inject private BambooService bambooService;

  @Override
  public List<BuildDetails> getBuilds(String appId, String artifactStreamId, BambooConfig bambooConfig) {
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    notNullCheck("artifactStream", artifactStream);
    equalCheck(artifactStream.getSourceType(), SourceType.BAMBOO);

    BambooArtifactStream bambooArtifactStream = ((BambooArtifactStream) artifactStream);
    return bambooService.getBuildsForJob(
        bambooConfig, bambooArtifactStream.getJobname(), 50); // read 50 from some config
  }

  @Override
  public Set<String> getJobs(BambooConfig bambooConfig) {
    return new HashSet<>(bambooService.getJobKeys(bambooConfig));
  }

  @Override
  public Set<String> getArtifactPaths(String jobName, BambooConfig bambooConfig) {
    return new HashSet<>(bambooService.getArtifactPath(bambooConfig, jobName));
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, String artifactStreamId, BambooConfig bambooConfig) {
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    notNullCheck("artifactStream", artifactStream);
    equalCheck(artifactStream.getSourceType(), SourceType.BAMBOO);

    BambooArtifactStream bambooArtifactStream = ((BambooArtifactStream) artifactStream);
    return bambooService.getLastSuccessfulBuild(bambooConfig, bambooArtifactStream.getJobname());
  }
}
