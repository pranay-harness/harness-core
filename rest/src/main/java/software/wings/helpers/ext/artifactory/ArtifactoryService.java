package software.wings.helpers.ext.artifactory;

import software.wings.beans.config.ArtifactoryConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Map;

/**
 * Created by sgurubelli on 6/27/17.
 */
public interface ArtifactoryService {
  /**
   * Gets builds.
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

  /***
   * Get GroupId paths
   * @param artifactoryConfig the Artifactory Config
   * @param repoKey
   * @return List of Repo paths or docker images
   */
  List<String> getRepoPaths(ArtifactoryConfig artifactoryConfig, String repoKey);
}
