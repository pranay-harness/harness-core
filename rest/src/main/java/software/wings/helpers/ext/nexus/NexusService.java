package software.wings.helpers.ext.nexus;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.config.NexusConfig;

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
   * @param buildNumber
   * @param artifactPathRegex
   * @return
   */
  Pair<String, InputStream> downloadArtifact(
      NexusConfig nexusConfig, String repoType, String buildNumber, String artifactPathRegex);

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
}
