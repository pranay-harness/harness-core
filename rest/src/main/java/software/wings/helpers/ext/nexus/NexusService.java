package software.wings.helpers.ext.nexus;

import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.config.NexusConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.ArtifactType;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by srinivas on 3/28/17.
 */
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
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, ArtifactType artifactType);

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

  /**
   * Download artifact pair
   *
   * @param nexusConfig
   * @param repoType
   * @param groupId
   * @param artifactNames
   * @return Input stream
   */
  Pair<String, InputStream> downloadArtifact(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoType, String groupId, String artifactNames);

  /**
   * Download artifact pair
   *
   * @param nexusConfig
   * @param repoType
   * @param groupId
   * @param artifactName
   * @param version
   * @return Input stream
   */
  Pair<String, InputStream> downloadArtifact(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoType, String groupId, String artifactName, String version);

  /***
   * Get GroupId paths
   * @param nexusConfig
   * @param repoId
   * @return
   */
  List<String> getGroupIdPaths(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId);

  /***
   *
   * @param nexusConfig
   * @param repoId the repoId
   * @param path the path
   * @return
   */
  List<String> getArtifactNames(
      NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId, String path);

  /**
   * @param nexusConfig
   * @param repoId       Repository Type
   * @param groupId      Group Id
   * @param artifactName artifact name
   * @return list of versions
   */
  List<BuildDetails> getVersions(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoId,
      String groupId, String artifactName);

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
   * @param repoKey
   * @param imageName
   * @param maxNumberOfBuilds
   * @return
   */
  List<BuildDetails> getBuilds(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails, String repoKey,
      String imageName, int maxNumberOfBuilds);

  /**
   *  Checks if it is connectable and valid credentials
   * @param nexusConfig
   * @param encryptionDetails
   * @return
   */
  boolean isRunning(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails);
}
