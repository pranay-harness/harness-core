package software.wings.service.intfc;

import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.ArtifactType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by sgurubelli on 6/20/17.
 */
public interface ArtifactoryBuildService extends BuildService<ArtifactoryConfig> {
  @DelegateTaskType(TaskType.ARTIFACTORY_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.ARTIFACTORY_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails, int limit);

  @DelegateTaskType(TaskType.ARTIFACTORY_GET_JOBS)
  List<JobDetails> getJobs(
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails, Optional<String> parentJobName);

  @DelegateTaskType(TaskType.ARTIFACTORY_GET_PLANS)
  Map<String, String> getPlans(ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.ARTIFACTORY_GET_PLANS)
  Map<String, String> getPlans(ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails,
      ArtifactType artifactType, String repositoryType);

  @DelegateTaskType(TaskType.ARTIFACTORY_GET_ARTIFACTORY_PATHS)
  List<String> getArtifactPaths(
      String jobName, String groupId, ArtifactoryConfig config, List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.ARTIFACTORY_GET_GROUP_IDS)
  List<String> getGroupIds(
      String repoType, ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.ARTIFACTORY_LAST_SUCCSSFUL_BUILD)
  BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      ArtifactoryConfig config, List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.ARTIFACTORY_VALIDATE_ARTIFACT_SERVER)
  boolean validateArtifactServer(ArtifactoryConfig artifactoryConfig);

  @DelegateTaskType(TaskType.ARTIFACTORY_VALIDATE_ARTIFACT_STREAM)
  boolean validateArtifactSource(ArtifactoryConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes);
}
