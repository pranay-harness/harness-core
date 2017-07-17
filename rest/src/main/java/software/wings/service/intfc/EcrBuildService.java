package software.wings.service.intfc;

import software.wings.beans.EcrConfig;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

/**
 * Created by brett on 7/16/17.
 */
public interface EcrBuildService extends BuildService<EcrConfig> {
  /**
   * Gets builds.
   *
   * @param appId                    the app id
   * @param artifactStreamAttributes the artifact stream attributes
   * @param ecrConfig             the ecr config
   * @return the builds
   */
  @DelegateTaskType(TaskType.DOCKER_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, EcrConfig ecrConfig);

  @DelegateTaskType(TaskType.DOCKER_VALIDATE_ARTIFACT_SERVER) boolean validateArtifactServer(EcrConfig config);

  @DelegateTaskType(TaskType.DOCKER_VALIDATE_ARTIFACT_STREAM)
  boolean validateArtifactSource(EcrConfig config, ArtifactStreamAttributes artifactStreamAttributes);
}
