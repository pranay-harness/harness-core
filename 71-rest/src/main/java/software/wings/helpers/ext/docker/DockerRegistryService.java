package software.wings.helpers.ext.docker;

import software.wings.beans.DockerConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

/**
 * Created by anubhaw on 1/6/17.
 */
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
