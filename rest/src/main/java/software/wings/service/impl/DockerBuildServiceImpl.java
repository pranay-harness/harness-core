package software.wings.service.impl;

import static software.wings.utils.Validator.equalCheck;

import software.wings.beans.DockerConfig;
import software.wings.beans.ErrorCodes;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.docker.DockerRegistryService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.DockerBuildService;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by anubhaw on 1/6/17.
 */
@Singleton
public class DockerBuildServiceImpl implements DockerBuildService {
  @Inject private DockerRegistryService dockerRegistryService;

  @Override
  public List<BuildDetails> getBuilds(String appId, ArtifactStream artifactStream, DockerConfig dockerConfig) {
    equalCheck(artifactStream.getArtifactStreamType(), ArtifactStreamType.DOCKER.name());
    DockerArtifactStream dockerArtifactStream = (DockerArtifactStream) artifactStream;
    List<BuildDetails> builds = dockerRegistryService.getBuilds(dockerConfig, dockerArtifactStream.getImageName(), 50);
    return builds;
  }

  @Override
  public List<String> getJobs(DockerConfig jenkinsConfig) {
    throw new WingsException(
        ErrorCodes.INVALID_REQUEST, "message", "Operation not supported by Docker Artifact Stream");
  }

  @Override
  public List<String> getArtifactPaths(String jobName, DockerConfig config) {
    throw new WingsException(
        ErrorCodes.INVALID_REQUEST, "message", "Operation not supported by Docker Artifact Stream");
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(String appId, ArtifactStream artifactStream, DockerConfig dockerConfig) {
    throw new WingsException(
        ErrorCodes.INVALID_REQUEST, "message", "Operation not supported by Docker Artifact Stream");
  }

  @Override
  public Map<String, String> getPlans(DockerConfig config) {
    throw new WingsException(
        ErrorCodes.INVALID_REQUEST, "message", "Operation not supported by Docker Artifact Stream");
  }
}
