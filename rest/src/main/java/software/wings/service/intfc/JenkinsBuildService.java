package software.wings.service.intfc;

import software.wings.beans.JenkinsConfig;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Map;

/**
 * Created by peeyushaggarwal on 1/12/17.
 */
public interface JenkinsBuildService extends BuildService<JenkinsConfig> {
  @DelegateTaskType(TaskType.JENKINS_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, JenkinsConfig config);

  @DelegateTaskType(TaskType.JENKINS_GET_JOBS) List<String> getJobs(JenkinsConfig jenkinsConfig);

  @DelegateTaskType(TaskType.JENKINS_GET_ARTIFACT_PATHS)
  List<String> getArtifactPaths(String jobName, String groupId, JenkinsConfig config);

  @DelegateTaskType(TaskType.JENKINS_LAST_SUCCESSFUL_BUILD)
  BuildDetails getLastSuccessfulBuild(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, JenkinsConfig config);

  @DelegateTaskType(TaskType.JENKINS_GET_PLANS) Map<String, String> getPlans(JenkinsConfig config);

  @DelegateTaskType(TaskType.JENKINS_VALIDATE_ARTIFACT_SERVER)
  boolean validateArtifactServer(JenkinsConfig jenkinsConfig);
}
