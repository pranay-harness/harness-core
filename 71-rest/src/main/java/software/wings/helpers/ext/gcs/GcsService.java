package software.wings.helpers.ext.gcs;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.GcpConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Map;

public interface GcsService {
  /**
   * List buckets
   *
   * @param gcpConfig GCP Config
   * @param projectId GCS Project Id
   * @param encryptedDataDetails Encryption details
   * @return List of buckets
   */
  Map<String, String> listBuckets(
      GcpConfig gcpConfig, String projectId, List<EncryptedDataDetail> encryptedDataDetails);

  /**
   * Get GCS project Id
   *
   * @param gcpConfig GCP Config
   * @param encryptedDataDetails Encryption details
   * @return GCS project
   */
  String getProject(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptedDataDetails);

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
      String bucketName, String artifactPath, boolean isExpression, boolean versioningEnabledForBucket, int limit);

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

  /**
   * Get the artifact related information
   *
   * @param gcpConfig     GCS cloud provider config
   * @param artifactStreamAttributes ArtifactStreamAttributes
   * @return
   */
  List<BuildDetails> getArtifactsBuildDetails(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes, List<String> artifactPaths, boolean isExpression, int limit);
}
