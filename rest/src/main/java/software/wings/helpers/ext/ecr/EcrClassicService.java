package software.wings.helpers.ext.ecr;

import software.wings.beans.EcrConfig;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

/**
 * Created by brett on 7/15/17
 */
public interface EcrClassicService {
  /**
   * Gets builds.
   *
   * @param ecrConfig         the ecr config
   * @param imageName         the image name
   * @param maxNumberOfBuilds the max number of builds
   * @return the builds
   */
  List<BuildDetails> getBuilds(
      EcrConfig ecrConfig, List<EncryptedDataDetail> encryptionDetails, String imageName, int maxNumberOfBuilds);

  /**
   * Gets last successful build.
   *
   * @param ecrConfig the ecr config
   * @param imageName the image name
   * @return the last successful build
   */
  BuildDetails getLastSuccessfulBuild(
      EcrConfig ecrConfig, List<EncryptedDataDetail> encryptionDetails, String imageName);

  /**
   * Validates the Image
   *
   * @param ecrConfig the ecr config
   * @param imageName the image name
   * @return the boolean
   */
  boolean verifyRepository(EcrConfig ecrConfig, List<EncryptedDataDetail> encryptionDetails, String imageName);

  /**
   * Validate the credentials
   *
   * @param ecrConfig the ecr config
   * @return boolean
   */
  boolean validateCredentials(EcrConfig ecrConfig, List<EncryptedDataDetail> encryptionDetails);

  /**
   * List ecr registry list.
   *
   * @param ecrConfig the ecr config
   * @return the list
   */
  List<String> listEcrRegistry(EcrConfig ecrConfig, List<EncryptedDataDetail> encryptionDetails);

  /**
   * Get the ECR repository info for the given name
   *
   * @param ecrConfig         ecr artifact server / connector config
   * @param ecrArtifactStream repository name
   * @return ecr image url
   */
  String getEcrImageUrl(EcrConfig ecrConfig, EcrArtifactStream ecrArtifactStream);
}