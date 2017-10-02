package software.wings.helpers.ext.artifactory;

import software.wings.beans.config.ArtifactoryConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.utils.ArtifactType;
import software.wings.waitnotify.ListNotifyResponseData;

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
  List<BuildDetails> getBuilds(
      ArtifactoryConfig artifactoryConfig, String repoKey, String imageName, int maxNumberOfBuilds);

  /**
   * Get versions
   * @param artifactoryConfig
   * @param repoKey
   * @param artifactName
   * @param artifactType
   * @return
   */
  List<BuildDetails> getFilePaths(ArtifactoryConfig artifactoryConfig, String repoKey, String groupId,
      String artifactName, ArtifactType artifactType, int maxVersions);

  /**
   * Get versions
   * @param artifactoryConfig
   * @param repoKey
   * @param artifactName
   * @param artifactType
   * @return
   */
  BuildDetails getLatestFilePath(ArtifactoryConfig artifactoryConfig, String repoKey, String groupId,
      String artifactName, ArtifactType artifactType);

  /**
   * Gets last successful build.
   *
   * @param artifactoryConfig the docker config
   * @param repositoryPath    the repository path
   * @return the last successful build
   */
  BuildDetails getLastSuccessfulBuild(ArtifactoryConfig artifactoryConfig, String repositoryPath);

  /**
   * Get Repositories
   * @return map RepoId and Name
   */
  Map<String, String> getRepositories(ArtifactoryConfig artifactoryConfig);

  /**
   * Get Repositories
   * @return map RepoId and Name
   */
  Map<String, String> getRepositories(ArtifactoryConfig artifactoryConfig, ArtifactType artifactType);

  /***
   * Get GroupId paths
   * @param artifactoryConfig the Artifactory Config
   * @param repoKey
   * @return List of Repo paths or docker images
   */
  List<String> getRepoPaths(ArtifactoryConfig artifactoryConfig, String repoKey);

  /***
   * Get all artifact ids
   * @param artifactoryConfig
   * @param repoId the repoId
   * @param path the path
   * @return
   */
  List<String> getArtifactIds(ArtifactoryConfig artifactoryConfig, String repoId, String path);

  /**
   * Gets the latest version of the given artifact
   * @param artifactoryConfig
   * @param repoId
   * @param groupId
   * @param artifactName
   * @return
   */
  BuildDetails getLatestVersion(
      ArtifactoryConfig artifactoryConfig, String repoId, String groupId, String artifactName);

  /**
   * Download artifacts
   * @param repoType
   * @param groupId
   * @param artifactIds
   * @param artifactPattern Artifact Pattern
   * @return Input stream
   */
  ListNotifyResponseData downloadArtifacts(ArtifactoryConfig artifactoryConfig, String repoType, String groupId,
      List<String> artifactIds, String artifactPattern, Map<String, String> metadata, String delegateId, String taskId,
      String accountId);
}
