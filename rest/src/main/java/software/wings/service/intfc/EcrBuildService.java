package software.wings.service.intfc;

import software.wings.beans.AwsConfig;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;

/**
 * Created by brett on 7/16/17.
 */
public interface EcrBuildService extends BuildService<AwsConfig> {
  /**
   * Gets builds.
   *
   * @param appId                    the app id
   * @param artifactStreamAttributes the artifact stream attributes
   * @param awsConfig                the aws cloud provider config
   * @return the builds
   */
  @DelegateTaskType(TaskType.ECR_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.ECR_VALIDATE_ARTIFACT_STREAM)
  boolean validateArtifactSource(
      AwsConfig config, List<EncryptedDataDetail> encryptionDetails, ArtifactStreamAttributes artifactStreamAttributes);

  @DelegateTaskType(TaskType.ECR_GET_PLANS)
  Map<String, String> getPlans(AwsConfig config, List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.ECR_GET_ARTIFACT_PATHS)
  List<String> getArtifactPaths(
      String region, String groupId, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails);
}
