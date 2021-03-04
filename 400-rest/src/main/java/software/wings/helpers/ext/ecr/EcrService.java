package software.wings.helpers.ext.ecr;

import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.beans.AwsInternalConfig;
import software.wings.beans.AwsConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Map;

import static io.harness.annotations.dev.HarnessTeam.CDC;

/**
 * Created by brett on 7/15/17
 */
@OwnedBy(CDC)
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
  List<BuildDetails> getBuilds(
      AwsInternalConfig awsConfig, String imageUrl, String region, String imageName, int maxNumberOfBuilds);

  /**
   * Gets last successful build.
   *
   * @param awsConfig the ecr config
   * @param imageName the image name
   * @return the last successful build
   */
  BuildDetails getLastSuccessfulBuild(AwsConfig awsConfig, String imageName);

  /**
   * Validates the Image
   *
   * @param awsConfig the ecr config
   * @param region    the aws region
   * @param imageName the image name
   * @return the boolean
   */
  boolean verifyRepository(AwsInternalConfig awsConfig, String region, String imageName);

  /**
   * Lists aws regions
   *
   * @param awsConfig aws config
   * @return
   */
  List<String> listRegions(AwsInternalConfig awsConfig);

  /**
   * List ecr registry list.
   *
   * @param awsConfig the ecr config
   * @return the list
   */
  List<String> listEcrRegistry(AwsInternalConfig awsConfig, String region);

  List<Map<String, String>> getLabels(AwsInternalConfig awsConfig, String imageName, String region, List<String> tags);
}
