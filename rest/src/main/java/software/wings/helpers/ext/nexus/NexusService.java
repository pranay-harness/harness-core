package software.wings.helpers.ext.nexus;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.config.NexusConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

/**
 * Created by srinivas on 3/28/17.
 */
public interface NexusService {
  /**
   * Get Repositories
   * @return map RepoId and Name
   */
  Map<String, String> getRepositories(final NexusConfig nexusConfig);

  /**
   * Get Artifact paths under repository
   * @param repoId
   * @return List<String> artifact paths
   */
  List<String> getArtifactPaths(NexusConfig nexusConfig, String repoId);

  /**
   * Get Artifact paths for a given repo from the given relative path
   * @param repoId
   * @return List<String> artifact paths
   */
  List<String> getArtifactPaths(NexusConfig nexusConfig, String repoId, String name);

  /**
   * Download artifact pair
   * @param nexusConfig
   * @param repoType
   * @param groupId
   * @param artifactNames
   * @return Input stream
   */
  Pair<String, InputStream> downloadArtifact(
      NexusConfig nexusConfig, String repoType, String groupId, String artifactNames);

  /**
   * Download artifact pair
   * @param nexusConfig
   * @param repoType
   * @param groupId
   * @param artifactName
   * @param version
   * @return Input stream
   */
  Pair<String, InputStream> downloadArtifact(
      NexusConfig nexusConfig, String repoType, String groupId, String artifactName, String version);

  /***
   * Get GroupId paths
   * @param nexusConfig
   * @param repoId
   * @return
   */
  List<String> getGroupIdPaths(NexusConfig nexusConfig, String repoId);

  /***
   *
   * @param nexusConfig
   * @param repoId the repoId
   * @param path the path
   * @return
   */
  List<String> getArtifactNames(NexusConfig nexusConfig, String repoId, String path);

  /**
   * @param nexusConfig
   * @param repoId Repository Type
   * @param groupId Group Id
   * @param  artifactName artifact name
   * @return list of versions
   *
   */
  List<BuildDetails> getVersions(NexusConfig nexusConfig, String repoId, String groupId, String artifactName);

  /**
   * Gets the latest version of the given artifact
   * @param nexusConfig
   * @param repoId
   * @param groupId
   * @param artifactName
   * @return
   */
  BuildDetails getLatestVersion(NexusConfig nexusConfig, String repoId, String groupId, String artifactName);
}
