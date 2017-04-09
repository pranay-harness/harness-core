package software.wings.service.intfc;

import java.util.List;
import java.util.Map;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.config.NexusConfig;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;

/**
 * Created by srinivas on 3/31/17.
 */
public interface NexusBuildService extends BuildService<NexusConfig> {
  @DelegateTaskType(TaskType.NEXUS_GET_JOBS) List<String> getJobs(NexusConfig config);

  @DelegateTaskType(TaskType.NEXUS_GET_PLANS) Map<String, String> getPlans(NexusConfig config);

  @DelegateTaskType(TaskType.NEXUS_GET_ARTIFACT_PATHS)
  List<String> getArtifactPaths(String jobName, String groupId, NexusConfig config);

  @DelegateTaskType(TaskType.NEXUS_GET_GROUP_IDS) List<String> getGroupIds(String repoType, NexusConfig config);

  @DelegateTaskType(TaskType.NEXUS_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, NexusConfig config);

  @DelegateTaskType(TaskType.NEXUS_LAST_SUCCESSFUL_BUILD)
  BuildDetails getLastSuccessfulBuild(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, NexusConfig config);
}
