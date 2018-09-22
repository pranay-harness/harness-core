package software.wings.helpers.ext.amazons3;

import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.AwsConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.waitnotify.ListNotifyResponseData;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * @author rktummala on 7/29/17.
 */
public interface AmazonS3Service {
  /**
   * Get Repositories
   *
   * @return map RepoId and Name
   */
  Map<String, String> getBuckets(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails);

  /**
   * Get artifact paths for a given repo from the given bucket.
   *
   * @param awsConfig  aws cloud provider config
   * @param bucketName s3 bucket name
   * @return
   */
  List<String> getArtifactPaths(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName);

  /**
   * downloads artifacts from s3 based on the given inputs
   *
   * @param awsConfig     aws config
   * @param bucketName    bucket name
   * @param artifactPaths artifact paths
   * @param delegateId    delegate id
   * @param taskId        task id
   * @param accountId     account id
   * @return
   */
  ListNotifyResponseData downloadArtifacts(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String bucketName, List<String> artifactPaths, String delegateId, String taskId, String accountId)
      throws IOException, URISyntaxException;

  Pair<String, InputStream> downloadArtifact(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName, String key);
  /**
   * Gets the artifact related information
   *
   * @param config     aws cloud provider config
   * @param bucketName bucket name
   * @param key        artifact path / key
   * @param versioningEnabledForBucket versioning enabled
   * @param artifactFileSize artifactFileSize
   * @return
   */
  BuildDetails getArtifactBuildDetails(AwsConfig config, List<EncryptedDataDetail> encryptionDetails, String bucketName,
      String key, boolean versioningEnabledForBucket, Long artifactFileSize);

  List<BuildDetails> getArtifactsBuildDetails(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String bucketName, List<String> artifactPaths, boolean isExpression);

  Long getFileSize(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName, String key);
}
