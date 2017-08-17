package software.wings.helpers.ext.ecr;

import com.amazonaws.services.ecr.model.Repository;
import software.wings.beans.AwsConfig;
import software.wings.beans.EcrConfig;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.helpers.ext.jenkins.BuildDetails;

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
  List<BuildDetails> getBuilds(AwsConfig awsConfig, String region, String imageName, int maxNumberOfBuilds);

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
   * @param region the aws region
   * @param imageName the image name
   * @return the boolean
   */
  boolean verifyRepository(AwsConfig awsConfig, String region, String imageName);

  /**
   * Lists aws regions
   * @param awsConfig aws config
   * @return
   */
  List<String> listRegions(AwsConfig awsConfig);

  /**
   * List ecr registry list.
   *
   * @param awsConfig the ecr config
   * @return the list
   */
  List<String> listEcrRegistry(AwsConfig awsConfig, String region);

  /**
   * Get the ECR repository info for the given name
   * @param awsConfig aws cloud provider config
   * @param region aws region
   * @param ecrArtifactStream repository name
   * @return ecr image url
   */
  String getEcrImageUrl(AwsConfig awsConfig, String region, EcrArtifactStream ecrArtifactStream);
}
