package software.wings.helpers.ext.docker;

import software.wings.beans.DockerConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

/**
 * Created by anubhaw on 1/6/17.
 */
public interface DockerRegistryService {
  /**
   * Gets builds.
   *
   * @param dockerConfig      the docker config
   * @param imageName         the image name
   * @param maxNumberOfBuilds the max number of builds
   * @return the builds
   */
  List<BuildDetails> getBuilds(DockerConfig dockerConfig, String imageName, int maxNumberOfBuilds);

  /**
   * Gets last successful build.
   *
   * @param dockerConfig the docker config
   * @param imageName    the image name
   * @return the last successful build
   */
  BuildDetails getLastSuccessfulBuild(DockerConfig dockerConfig, String imageName);

  /**
   * Validates the Image
   * @param dockerConfig
   * @param imageName
   */
  void verifyImageName(DockerConfig dockerConfig, String imageName);
}
