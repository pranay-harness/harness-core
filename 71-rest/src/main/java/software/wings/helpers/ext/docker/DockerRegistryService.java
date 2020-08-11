package software.wings.helpers.ext.docker;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.docker.beans.DockerInternalConfig;

import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 1/6/17.
 */
@OwnedBy(CDC)
public interface DockerRegistryService {
  int MAX_NO_OF_TAGS_PER_IMAGE = 10000;

  /**
   * Gets builds.
   *
   * @param dockerConfig      the docker config
   * @param imageName         the image name
   * @param maxNumberOfBuilds the max number of builds
   * @return the builds
   */
  List<BuildDetailsInternal> getBuilds(DockerInternalConfig dockerConfig, String imageName, int maxNumberOfBuilds);

  /**
   * Gets labels.
   *
   * @param dockerConfig the docker config
   * @param imageName    the image name
   * @param tags         the image tags to find labels of
   * @return the builds
   */
  List<Map<String, String>> getLabels(DockerInternalConfig dockerConfig, String imageName, List<String> tags);

  /**
   * Gets last successful build.
   *
   * @param dockerConfig the docker config
   * @param imageName    the image name
   * @return the last successful build
   */
  BuildDetailsInternal getLastSuccessfulBuild(DockerInternalConfig dockerConfig, String imageName);

  /**
   * Validates the Image
   * @param dockerConfig the docker config
   * @param imageName the image name
   */
  boolean verifyImageName(DockerInternalConfig dockerConfig, String imageName);

  /**
   * Validate the credentials
   *
   * @param dockerConfig the docker config
   * @return boolean validate
   */
  boolean validateCredentials(DockerInternalConfig dockerConfig);
}
