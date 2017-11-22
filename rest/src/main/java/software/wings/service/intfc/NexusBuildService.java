package software.wings.service.intfc;

import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.config.NexusConfig;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.ArtifactType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by srinivas on 3/31/17.
 */
public interface NexusBuildService extends BuildService<NexusConfig> {
  @DelegateTaskType(TaskType.NEXUS_GET_JOBS)
  List<JobDetails> getJobs(
      NexusConfig config, List<EncryptedDataDetail> encryptionDetails, Optional<String> parentJobName);

  @DelegateTaskType(TaskType.NEXUS_GET_PLANS)
  Map<String, String> getPlans(NexusConfig config, List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.NEXUS_GET_PLANS)
  Map<String, String> getPlans(NexusConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactType artifactType, String repositoryType);

  @DelegateTaskType(TaskType.NEXUS_GET_ARTIFACT_PATHS)
  List<String> getArtifactPaths(
      String jobName, String groupId, NexusConfig config, List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.NEXUS_GET_GROUP_IDS)
  List<String> getGroupIds(String repoType, NexusConfig config, List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.NEXUS_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, NexusConfig config,
      List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.NEXUS_LAST_SUCCESSFUL_BUILD)
  BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      NexusConfig config, List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.NEXUS_VALIDATE_ARTIFACT_SERVER) boolean validateArtifactServer(NexusConfig config);
}
