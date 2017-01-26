package software.wings.service.impl;

import static software.wings.utils.Validator.equalCheck;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import software.wings.beans.BambooConfig;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.BambooBuildService;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * Created by anubhaw on 11/22/16.
 */
@Singleton
public class BambooBuildServiceImpl implements BambooBuildService {
  @Inject private BambooService bambooService;

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStream artifactStream, BambooConfig bambooConfig) {
    equalCheck(artifactStream.getArtifactStreamType(), ArtifactStreamType.BAMBOO);

    BambooArtifactStream bambooArtifactStream = ((BambooArtifactStream) artifactStream);
    return bambooService.getBuilds(bambooConfig, bambooArtifactStream.getJobname(), 50); // read 50 from some config
  }

  @Override
  public List<String> getJobs(BambooConfig bambooConfig) {
    return Lists.newArrayList(bambooService.getPlanKeys(bambooConfig).keySet());
  }

  @Override
  public Map<String, String> getPlans(BambooConfig bambooConfig) {
    return bambooService.getPlanKeys(bambooConfig);
  }

  @Override
  public List<String> getArtifactPaths(String jobName, BambooConfig bambooConfig) {
    return bambooService.getArtifactPath(bambooConfig, jobName);
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, ArtifactStream artifactStream, BambooConfig bambooConfig) {
    equalCheck(artifactStream.getArtifactStreamType(), ArtifactStreamType.BAMBOO);

    BambooArtifactStream bambooArtifactStream = ((BambooArtifactStream) artifactStream);
    return bambooService.getLastSuccessfulBuild(bambooConfig, bambooArtifactStream.getJobname());
  }
}
