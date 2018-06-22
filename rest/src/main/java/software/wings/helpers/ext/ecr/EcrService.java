package software.wings.helpers.ext.ecr;

import software.wings.beans.AwsConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

/**
 * Created by brett on 7/15/17
 */
public interface EcrService {
  /**
   * Gets builds.
   *
   * @param awsConfig         the aws cloud provider config
   * @param region            the region name
   * @param imageName         the image name
   * @param maxNumberOfBuilds the max number of builds
   * @return the builds
   */
  List<BuildDetails> getBuilds(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String imageName, int maxNumberOfBuilds);

  /**
   * Gets last successful build.
   *
   * @param awsConfig the ecr config
   * @param imageName the image name
   * @return the last successful build
   */
  BuildDetails getLastSuccessfulBuild(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String imageName);

  /**
   * Validates the Image
   *
   * @param awsConfig the ecr config
   * @param region    the aws region
   * @param imageName the image name
   * @return the boolean
   */
  boolean verifyRepository(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String imageName);

  /**
   * Lists aws regions
   *
   * @param awsConfig aws config
   * @return
   */
  List<String> listRegions(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails);

  /**
   * List ecr registry list.
   *
   * @param awsConfig the ecr config
   * @return the list
   */
  List<String> listEcrRegistry(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);
}
