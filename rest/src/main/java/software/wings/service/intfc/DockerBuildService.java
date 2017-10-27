package software.wings.service.intfc;

import software.wings.beans.DockerConfig;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

/**
 * Created by anubhaw on 1/6/17.
 */
public interface DockerBuildService extends BuildService<DockerConfig> {
  /**
   * Gets builds.
   *
   * @param appId                    the app id
   * @param artifactStreamAttributes the artifact stream attributes
   * @param dockerConfig             the docker config
   * @return the builds
   */
  @DelegateTaskType(TaskType.DOCKER_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.DOCKER_VALIDATE_ARTIFACT_SERVER) boolean validateArtifactServer(DockerConfig config);

  @DelegateTaskType(TaskType.DOCKER_VALIDATE_ARTIFACT_STREAM)
  boolean validateArtifactSource(DockerConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes);
}
