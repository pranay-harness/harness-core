package software.wings.service.intfc;

import software.wings.beans.DockerConfig;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

/**
 * Created by anubhaw on 1/6/17.
 */
public interface DockerBuildService extends BuildService<DockerConfig> {
  /**
   * Gets builds.
   *
   * @param appId                    the app id
   * @param ArtifactStreamAttributes the artifact stream attributes
   * @param dockerConfig             the docker config
   * @return the builds
   */
  @DelegateTaskType(TaskType.DOCKER_GET_BUILDS)
  List<BuildDetails> getBuilds(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, DockerConfig dockerConfig);
}
