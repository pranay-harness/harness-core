package software.wings.helpers.ext.amazons3;

import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.AwsConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * @author rktummala on 7/29/17.
 */
public interface AmazonS3Service {
  /**
   * Get Repositories
   * @return map RepoId and Name
   */
  Map<String, String> getBuckets(final AwsConfig awsConfig);

  /**
   * Get artifact paths for a given repo from the given bucket.
   * @param awsConfig aws cloud provider config
   * @param bucketName s3 bucket name
   * @return
   */
  List<String> getArtifactPaths(AwsConfig awsConfig, String bucketName);

  /**
   * Downloads the artifact from S3
   *
   * @param awsConfig aws cloud provider config
   * @param bucketName s3 bucket name
   * @param artifactPath s3 artifact path
   * @return Returns a pair of artifact key and the input stream
   */
  Pair<String, InputStream> downloadArtifact(AwsConfig awsConfig, String bucketName, String artifactPath);

  /**
   * Gets the artifact related information
   * @param config aws cloud provider config
   * @param artifactStreamAttributes artifact stream attributes
   * @param appId application id
   * @return artifact info wrapped in build details
   */
  BuildDetails getArtifactMetadata(AwsConfig config, ArtifactStreamAttributes artifactStreamAttributes, String appId);
}
