package software.wings.helpers.ext.artifactory;

import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.ArtifactType;
import software.wings.waitnotify.ListNotifyResponseData;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by sgurubelli on 6/27/17.
 */
public interface ArtifactoryService {
  /**
   * Gets docker images
   *
   * @param artifactoryConfig  the artifactory config
   * @param repoKey     the repo key
   * @param imageName the image name
   * @param maxNumberOfBuilds the max number of builds
   * @return the builds
   */
  List<BuildDetails> getBuilds(ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoKey, String imageName, int maxNumberOfBuilds);

  /**
   *
   * @param artifactoryConfig
   * @param encryptionDetails
   * @param repositoryName
   * @param artifactPath
   * @param repositoryType
   * @param maxVersions
   * @return
   */
  List<BuildDetails> getFilePaths(ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails,
      String repositoryName, String artifactPath, String repositoryType, int maxVersions);

  /**
   * Get Repositories
   * @return map RepoId and Name
   */
  Map<String, String> getRepositories(ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails);

  /**
   * Get Repositories
   * @return map RepoId and Name
   */
  Map<String, String> getRepositories(
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails, ArtifactType artifactType);

  /**
   * Get Repositories
   * @return map RepoId and Name
   */
  Map<String, String> getRepositories(
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails, String packageType);

  /***
   * Get docker tags
   * @param artifactoryConfig the Artifactory Config
   * @param repoKey
   * @return List of Repo paths or docker images
   */
  List<String> getRepoPaths(
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails, String repoKey);

  /**
   * Download artifacts
   * @param repoType
   * @return Input stream
   */
  ListNotifyResponseData downloadArtifacts(ArtifactoryConfig artifactoryConfig,
      List<EncryptedDataDetail> encryptionDetails, String repoType, Map<String, String> metadata, String delegateId,
      String taskId, String accountId);

  Pair<String, InputStream> downloadArtifact(ArtifactoryConfig artifactoryConfig,
      List<EncryptedDataDetail> encryptionDetails, String repositoryName, Map<String, String> metadata);

  boolean validateArtifactPath(ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoType, String artifactPath, String repositoryType);

  Long getFileSize(
      ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails, Map<String, String> metadata);

  boolean isRunning(ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptionDetails);
}
