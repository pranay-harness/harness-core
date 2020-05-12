package software.wings.helpers.ext.azure.devops;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.waiter.ListNotifyResponseData;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.settings.azureartifacts.AzureArtifactsConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
public interface AzureArtifactsService {
  boolean validateArtifactServer(
      AzureArtifactsConfig azureArtifactsConfig, List<EncryptedDataDetail> encryptionDetails, boolean validateUrl);

  boolean validateArtifactSource(AzureArtifactsConfig azureArtifactsConfig, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes);

  List<BuildDetails> getBuilds(ArtifactStreamAttributes artifactStreamAttributes,
      AzureArtifactsConfig azureArtifactsConfig, List<EncryptedDataDetail> encryptionDetails);

  List<AzureDevopsProject> listProjects(
      AzureArtifactsConfig azureArtifactsConfig, List<EncryptedDataDetail> encryptionDetails);

  List<AzureArtifactsFeed> listFeeds(
      AzureArtifactsConfig azureArtifactsConfig, List<EncryptedDataDetail> encryptionDetails, String project);

  List<AzureArtifactsPackage> listPackages(AzureArtifactsConfig azureArtifactsConfig,
      List<EncryptedDataDetail> encryptionDetails, String project, String feed, String protocolType);

  List<AzureArtifactsPackageFileInfo> listFiles(AzureArtifactsConfig azureArtifactsConfig,
      List<EncryptedDataDetail> encryptionDetails, ArtifactStreamAttributes artifactStreamAttributes,
      Map<String, String> artifactMetadata, boolean nameOnly);

  void downloadArtifact(AzureArtifactsConfig azureArtifactsConfig, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes, Map<String, String> artifactMetadata, String delegateId,
      String taskId, String accountId, ListNotifyResponseData notifyResponseData);

  Pair<String, InputStream> downloadArtifact(AzureArtifactsConfig azureArtifactsConfig,
      List<EncryptedDataDetail> encryptionDetails, ArtifactStreamAttributes artifactStreamAttributes,
      Map<String, String> artifactMetadata);
}
