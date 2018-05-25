package software.wings.helpers.ext.gcs;

import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;

public interface GcsService {
  /**
   * Validate Settings Attribute and get Credentials
   *
   * @return GCP config
   */
  GcpConfig validateAndGetCredentials(SettingAttribute settingAttribute);

  /**
   * Create Bucket
   * @param gcpConfig  GCS config
   * @param encryptedDataDetails Encryption details
   * @param bucketName Bucket to create
   * @return none
   */
  void createBucket(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptedDataDetails, String bucketName);

  /**
   * Delete Bucket
   * @param gcpConfig  GCS config
   * @param encryptedDataDetails Encryption details
   * @param bucketName Bucket to delete
   * @return none
   */
  void deleteBucket(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptedDataDetails, String bucketName);

  /**
   * List Buckets
   * @param gcpConfig  GCS config
   * @param encryptedDataDetails Encryption details
   * @return map bucket name and Id
   */
  Map<String, String> listBuckets(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptedDataDetails);

  /**
   * Get artifact paths for a given repo from the given bucket.
   *
   * @param gcpConfig  GCS config
   * @param bucketName bucket name
   * @return
   */
  List<String> getArtifactPaths(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName);

  /**
   * Gets the artifact related information
   *
   * @param gcpConfig     GCS cloud provider config
   * @param bucketName bucket name
   * @return
   */
  List<BuildDetails> getArtifactsBuildDetails(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails,
      String bucketName, String artifactPath, boolean isExpression, boolean versioningEnabledForBucket);

  /**
   * Gets the artifact related information
   *
   * @param gcpConfig     GCS cloud provider config
   * @param bucketName bucket name
   * @return
   */
  List<BuildDetails> getArtifactsBuildDetails(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails,
      String bucketName, List<String> artifactPaths, boolean isExpression);

  /**
   * Gets the artifact related information
   *
   * @param gcpConfig     GCS cloud provider config
   * @param bucketName bucket name
   * @param objName object name
   * @return
   */
  BuildDetails getArtifactBuildDetails(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails,
      String bucketName, String objName, boolean versioningEnabledForBucket);
}
