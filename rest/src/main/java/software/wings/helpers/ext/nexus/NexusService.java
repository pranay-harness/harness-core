package software.wings.helpers.ext.nexus;

import java.util.List;
import java.util.Map;
import org.sonatype.nexus.rest.model.ContentListResource;
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
  List<String> getArtifactPaths(final NexusConfig nexusConfig, final String repoId);

  /**
   * Get Artifact paths for a given repo from the given relative path
   * @param repoId
   * @return List<String> artifact paths
   */
  List<String> getArtifactPaths(final NexusConfig nexusConfig, final String repoId, final String relativePath);
}
