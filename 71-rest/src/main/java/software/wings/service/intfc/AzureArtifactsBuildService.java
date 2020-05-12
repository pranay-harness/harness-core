package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.settings.azureartifacts.AzureArtifactsConfig;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.azure.devops.AzureArtifactsFeed;
import software.wings.helpers.ext.azure.devops.AzureArtifactsPackage;
import software.wings.helpers.ext.azure.devops.AzureDevopsProject;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

@OwnedBy(CDC)
public interface AzureArtifactsBuildService extends BuildService<AzureArtifactsConfig> {
  @Override
  @DelegateTaskType(TaskType.AZURE_ARTIFACTS_VALIDATE_ARTIFACT_SERVER)
  boolean validateArtifactServer(
      AzureArtifactsConfig azureArtifactsConfig, List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.AZURE_ARTIFACTS_VALIDATE_ARTIFACT_STREAM)
  boolean validateArtifactSource(AzureArtifactsConfig azureArtifactsConfig, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes);

  @Override
  @DelegateTaskType(TaskType.AZURE_ARTIFACTS_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      AzureArtifactsConfig azureArtifactsConfig, List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.AZURE_ARTIFACTS_GET_PROJECTS)
  List<AzureDevopsProject> getProjects(
      AzureArtifactsConfig azureArtifactsConfig, List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.AZURE_ARTIFACTS_GET_FEEDS)
  List<AzureArtifactsFeed> getFeeds(
      AzureArtifactsConfig azureArtifactsConfig, List<EncryptedDataDetail> encryptionDetails, String project);

  @Override
  @DelegateTaskType(TaskType.AZURE_ARTIFACTS_GET_PACKAGES)
  List<AzureArtifactsPackage> getPackages(AzureArtifactsConfig azureArtifactsConfig,
      List<EncryptedDataDetail> encryptionDetails, String project, String feed, String protocolType);
}
