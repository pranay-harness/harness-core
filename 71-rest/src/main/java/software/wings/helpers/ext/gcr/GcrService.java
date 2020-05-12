package software.wings.helpers.ext.gcr;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.GcpConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

/**
 * Created by brett on 8/2/17
 */
@OwnedBy(CDC)
public interface GcrService {
  /**
   * Gets builds.
   *
   * @param gcpConfig         the gcp cloud provider config
   * @param maxNumberOfBuilds the max number of builds
   * @return the builds
   */
  List<BuildDetails> getBuilds(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes, int maxNumberOfBuilds);

  /**
   * Gets last successful build.
   *
   * @param gcpConfig the gcr config
   * @return the last successful build
   */
  BuildDetails getLastSuccessfulBuild(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String imageName);

  /**
   * Validates the Image
   *
   * @param gcpConfig
   */
  boolean verifyImageName(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes);

  /**
   * Validate the credentials
   *
   * @param gcpConfig
   * @return
   */
  boolean validateCredentials(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes);
}
