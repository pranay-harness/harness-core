package software.wings.helpers.ext.ecr;

import software.wings.beans.EcrConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

/**
 * Created by anubhaw on 1/6/17.
 */
public interface EcrService {
  /**
   * Gets builds.
   *
   * @param ecrConfig      the ecr config
   * @param imageName         the image name
   * @param maxNumberOfBuilds the max number of builds
   * @return the builds
   */
  List<BuildDetails> getBuilds(EcrConfig ecrConfig, String imageName, int maxNumberOfBuilds);

  /**
   * Gets last successful build.
   *
   * @param ecrConfig the ecr config
   * @param imageName    the image name
   * @return the last successful build
   */
  BuildDetails getLastSuccessfulBuild(EcrConfig ecrConfig, String imageName);

  /**
   * Validates the Image
   * @param ecrConfig
   * @param imageName
   */
  boolean verifyImageName(EcrConfig ecrConfig, String imageName);

  /**
   * Validate the credentials
   * @param ecrConfig
   * @return
   */
  boolean validateCredentials(EcrConfig ecrConfig);
}
