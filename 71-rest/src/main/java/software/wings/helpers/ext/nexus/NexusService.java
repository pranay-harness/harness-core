package software.wings.helpers.ext.nexus;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.waiter.ListNotifyResponseData;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.config.NexusConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by srinivas on 3/28/17.
 */
@OwnedBy(CDC)
public interface NexusService {
  /**
   * Get Repositories
   *
   * @return map RepoId and Name
   */
  Map<String, String> getRepositories(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails);

  /**
   * Get Repositories
   *
   * @return map RepoId and Name
   */
  Map<String, String> getRepositories(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repositoryFormat);

  /**
   * Get Artifact paths under repository
   *
   * @param repoId
   * @return List&lt;String&gt; artifact paths
   */
  List<String> getArtifactPaths(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId);

  /**
   * Get Artifact paths for a given repo from the given relative path
   *
   * @param repoId
   * @return List&lt;String&gt; artifact paths
   */
  List<String> getArtifactPaths(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId, String name);

  Pair<String, InputStream> downloadArtifacts(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes, Map<String, String> metadata, String delegateId, String taskId,
      String accountId, ListNotifyResponseData res);

  /***
   * Get GroupId paths
   * @param nexusConfig
   * @param repoId
   * @return
   */
  List<String> getGroupIdPaths(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId, String repositoryFormat);

  /***
   *
   * @param nexusConfig
   * @param repoId the repoId
   * @param path the path
   * @return
   */
  List<String> getArtifactNames(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId, String path);

  /***
   *
   * @param nexusConfig
   * @param repoId the repoId
   * @param path the path
   * @param repositoryFormat the repositoryFormat
   * @return
   */
  List<String> getArtifactNames(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId,
      String path, String repositoryFormat);

  /**
   * @param nexusConfig
   * @param repoId       Repository Type
   * @param groupId      Group Id
   * @param artifactName artifact name
   * @param extension    extension
   * @param classifier   classifier
   * @return list of versions
   */
  List<BuildDetails> getVersions(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId,
      String groupId, String artifactName, String extension, String classifier);

  List<BuildDetails> getVersions(String repositoryFormat, NexusConfig nexusConfig,
      List<EncryptedDataDetail> encryptionDetails, String repoId, String packageName);

  /**
   * Gets the latest version of the given artifact
   *
   * @param nexusConfig
   * @param repoId
   * @param groupId
   * @param artifactName
   * @return
   */
  BuildDetails getLatestVersion(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId,
      String groupId, String artifactName);

  /**
   *
   * @param nexusConfig
   * @param encryptionDetails
   * @param artifactStreamAttributes
   * @param maxNumberOfBuilds
   * @return
   */
  List<BuildDetails> getBuilds(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes, int maxNumberOfBuilds);

  /**
   *  Checks if it is connectable and valid credentials
   * @param nexusConfig
   * @param encryptionDetails
   * @return
   */
  boolean isRunning(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails);

  /**
   * @param nexusConfig
   * @param repoId       Repository Type
   * @param groupId      Group Id
   * @param artifactName artifact name
   * @param extension    extension
   * @param classifier   classifier
   * @return true if versions exist
   */
  boolean existsVersion(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId,
      String groupId, String artifactName, String extension, String classifier);

  Pair<String, InputStream> downloadArtifactByUrl(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String artifactName, String artifactUrl);

  long getFileSize(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String artifactName, String artifactUrl);
}
