package software.wings.service.intfc;

import software.wings.beans.DockerConfig;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

/**
 * Created by anubhaw on 1/6/17.
 */
public interface DockerBuildService {
  /**
   * Gets builds.
   *
   * @param appId          the app id
   * @param artifactStream the artifact stream
   * @param dockerConfig   the docker config
   * @return the builds
   */
  List<BuildDetails> getBuilds(String appId, ArtifactStream artifactStream, DockerConfig dockerConfig);

  /**
   * Gets last successful build.
   *
   * @param appId          the app id
   * @param artifactStream the artifact stream
   * @param dockerConfig   the docker config
   * @return the last successful build
   */
  BuildDetails getLastSuccessfulBuild(String appId, ArtifactStream artifactStream, DockerConfig dockerConfig);
}
