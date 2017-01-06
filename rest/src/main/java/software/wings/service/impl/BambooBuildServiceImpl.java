package software.wings.service.impl;

import static software.wings.utils.Validator.equalCheck;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Singleton;

import software.wings.beans.BambooConfig;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.ArtifactStreamType;
import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BambooBuildService;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

/**
 * Created by anubhaw on 11/22/16.
 */
@Singleton
public class BambooBuildServiceImpl implements BambooBuildService {
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private BambooService bambooService;

  @Override
  public List<BuildDetails> getBuilds(String appId, String artifactStreamId, BambooConfig bambooConfig) {
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    notNullCheck("artifactStream", artifactStream);
    equalCheck(artifactStream.getArtifactStreamType(), ArtifactStreamType.BAMBOO);

    BambooArtifactStream bambooArtifactStream = ((BambooArtifactStream) artifactStream);
    return bambooService.getBuilds(bambooConfig, bambooArtifactStream.getJobname(), 50); // read 50 from some config
  }

  @Override
  public Set<String> getJobs(BambooConfig bambooConfig) {
    return bambooService.getPlanKeys(bambooConfig).keySet();
  }

  @Override
  public Map<String, String> getPlans(BambooConfig bambooConfig) {
    return bambooService.getPlanKeys(bambooConfig);
  }

  @Override
  public Set<String> getArtifactPaths(String jobName, BambooConfig bambooConfig) {
    return new HashSet<>(bambooService.getArtifactPath(bambooConfig, jobName));
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, String artifactStreamId, BambooConfig bambooConfig) {
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    notNullCheck("artifactStream", artifactStream);
    equalCheck(artifactStream.getArtifactStreamType(), ArtifactStreamType.BAMBOO);

    BambooArtifactStream bambooArtifactStream = ((BambooArtifactStream) artifactStream);
    return bambooService.getLastSuccessfulBuild(bambooConfig, bambooArtifactStream.getJobname());
  }
}
