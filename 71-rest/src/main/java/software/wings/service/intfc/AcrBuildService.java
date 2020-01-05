package software.wings.service.intfc;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.AzureConfig;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Map;

public interface AcrBuildService extends BuildService<AzureConfig> {
  @Override
  @DelegateTaskType(TaskType.ACR_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, AzureConfig azureConfig,
      List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.ACR_VALIDATE_ARTIFACT_STREAM)
  boolean validateArtifactSource(AzureConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes);

  @Override
  @DelegateTaskType(TaskType.ACR_GET_PLANS)
  Map<String, String> getPlans(AzureConfig config, List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.ACR_GET_ARTIFACT_PATHS)
  List<String> getArtifactPaths(
      String subscriptionId, String groupId, AzureConfig config, List<EncryptedDataDetail> encryptionDetails);
}
