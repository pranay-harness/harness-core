package software.wings.service.intfc;

import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Map;

/**
 * Created by sgurubelli on 6/20/17.
 */
public interface ArtifactoryBuildService extends BuildService<ArtifactoryConfig> {
  /**
   * Gets builds.
   *
   * @param appId                    the app id
   * @param artifactStreamAttributes the artifact stream attributes
   * @param artifactoryConfig        the artifactory config
   * @return the builds
   */
  @DelegateTaskType(TaskType.ARTIFACTORY_GET_BUILDS)
  List<BuildDetails> getBuilds(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, ArtifactoryConfig artifactoryConfig);

  @DelegateTaskType(TaskType.ARTIFACTORY_GET_JOBS) List<String> getJobs(ArtifactoryConfig artifactoryConfig);

  @DelegateTaskType(TaskType.ARTIFACTORY_GET_PLANS) Map<String, String> getPlans(ArtifactoryConfig artifactoryConfig);

  @DelegateTaskType(TaskType.ARTIFACTORY_GET_GROUP_IDS)
  List<String> getGroupIds(String repoType, ArtifactoryConfig artifactoryConfig);

  @DelegateTaskType(TaskType.ARTIFACTORY_VALIDATE_ARTIFACT_SERVER)
  boolean validateArtifactServer(ArtifactoryConfig artifactoryConfig);

  @DelegateTaskType(TaskType.ARTIFACTORY_VALIDATE_ARTIFACT_STREAM)
  boolean validateArtifactSource(ArtifactoryConfig config, ArtifactStreamAttributes artifactStreamAttributes);
}
