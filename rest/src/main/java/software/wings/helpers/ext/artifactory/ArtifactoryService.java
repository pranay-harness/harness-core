package software.wings.helpers.ext.artifactory;

import software.wings.beans.config.ArtifactoryConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

/**
 * Created by sgurubelli on 6/27/17.
 */
public interface ArtifactoryService {
  /**
   * Gets builds.
   *
   * @param artifactoryConfig  the artifactory config
   * @param repositoryPath     the repository path
   * @param maxNumberOfBuilds the max number of builds
   * @return the builds
   */
  List<BuildDetails> getBuilds(ArtifactoryConfig artifactoryConfig, String repositoryPath, int maxNumberOfBuilds);

  /**
   * Gets last successful build.
   *
   * @param artifactoryConfig the docker config
   * @param repositoryPath    the repository path
   * @return the last successful build
   */
  BuildDetails getLastSuccessfulBuild(ArtifactoryConfig artifactoryConfig, String repositoryPath);
}
