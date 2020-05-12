package software.wings.helpers.ext.docker;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.DockerConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

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
  List<BuildDetails> getBuilds(
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails, String imageName, int maxNumberOfBuilds);

  /**
   * Gets labels.
   *
   * @param dockerConfig the docker config
   * @param imageName    the image name
   * @param tags         the image tags to find labels of
   * @return the builds
   */
  List<Map<String, String>> getLabels(
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails, String imageName, List<String> tags);

  /**
   * Gets last successful build.
   *
   * @param dockerConfig the docker config
   * @param imageName    the image name
   * @return the last successful build
   */
  BuildDetails getLastSuccessfulBuild(
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails, String imageName);

  /**
   * Validates the Image
   *
   * @param dockerConfig
   * @param imageName
   */
  boolean verifyImageName(DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails, String imageName);

  /**
   * Validate the credentials
   *
   * @param dockerConfig
   * @return
   */
  boolean validateCredentials(DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails);
}
