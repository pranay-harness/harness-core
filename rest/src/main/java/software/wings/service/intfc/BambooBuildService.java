package software.wings.service.intfc;

import software.wings.beans.BambooConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Map;

/**
 * Created by peeyushaggarwal on 1/12/17.
 */
public interface BambooBuildService extends BuildService<BambooConfig> {
  @DelegateTaskType(TaskType.BAMBOO_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, String artifactSourceId, BambooConfig config);

  @DelegateTaskType(TaskType.BAMBOO_GET_JOBS) List<String> getJobs(BambooConfig jenkinsConfig);

  @DelegateTaskType(TaskType.BAMBOO_GET_ARTIFACT_PATHS)
  List<String> getArtifactPaths(String jobName, BambooConfig config);

  @DelegateTaskType(TaskType.BAMBOO_LAST_SUCCESSFUL_BUILD)
  BuildDetails getLastSuccessfulBuild(String appId, String artifactStreamId, BambooConfig config);

  @DelegateTaskType(TaskType.BAMBOO_GET_PLANS) Map<String, String> getPlans(BambooConfig config);
}
