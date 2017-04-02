package software.wings.service.intfc;

import java.util.List;
import java.util.Map;
import software.wings.beans.TaskType;
import software.wings.beans.config.NexusConfig;
import software.wings.delegatetasks.DelegateTaskType;

/**
 * Created by srinivas on 3/31/17.
 */
public interface NexusBuildService extends BuildService<NexusConfig> {
  @DelegateTaskType(TaskType.NEXUS_GET_JOBS) List<String> getJobs(NexusConfig config);

  @DelegateTaskType(TaskType.NEXUS_GET_PLANS) Map<String, String> getPlans(NexusConfig config);

  @DelegateTaskType(TaskType.NEXUS_GET_ARTIFACT_PATHS)
  List<String> getArtifactPaths(String jobName, NexusConfig config);
}
