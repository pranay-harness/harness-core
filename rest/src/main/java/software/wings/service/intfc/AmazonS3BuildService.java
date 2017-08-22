package software.wings.service.intfc;

import software.wings.beans.AwsConfig;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.config.NexusConfig;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author rktummala on 7/30/17.
 */
public interface AmazonS3BuildService extends BuildService<AwsConfig> {
  @DelegateTaskType(TaskType.AMAZON_S3_GET_PLANS) Map<String, String> getPlans(AwsConfig config);

  @DelegateTaskType(TaskType.AMAZON_S3_GET_ARTIFACT_PATHS)
  List<String> getArtifactPaths(String bucketName, String groupId, AwsConfig config);

  @DelegateTaskType(TaskType.AMAZON_S3_LAST_SUCCESSFUL_BUILD)
  BuildDetails getLastSuccessfulBuild(
      String appId, ArtifactStreamAttributes artifactStreamAttributes, AwsConfig config);
}
