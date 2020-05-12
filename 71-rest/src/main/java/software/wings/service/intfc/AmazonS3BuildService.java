package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.AwsConfig;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Map;

/**
 * @author rktummala on 7/30/17.
 */
@OwnedBy(CDC)
public interface AmazonS3BuildService extends BuildService<AwsConfig> {
  @Override
  @DelegateTaskType(TaskType.AMAZON_S3_GET_PLANS)
  Map<String, String> getPlans(AwsConfig config, List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.AMAZON_S3_GET_ARTIFACT_PATHS)
  List<String> getArtifactPaths(
      String bucketName, String groupId, AwsConfig config, List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.AMAZON_S3_LAST_SUCCESSFUL_BUILD)
  BuildDetails getLastSuccessfulBuild(String appId, ArtifactStreamAttributes artifactStreamAttributes, AwsConfig config,
      List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.AMAZON_S3_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails);
}
