package software.wings.helpers.ext.gcr;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.gcr.beans.GcpInternalConfig;

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
  List<BuildDetailsInternal> getBuilds(GcpInternalConfig gcpConfig, String imageName, int maxNumberOfBuilds);

  /**
   * Gets last successful build.
   *
   * @param gcpConfig the gcr config
   * @return the last successful build`
   */
  BuildDetailsInternal getLastSuccessfulBuild(GcpInternalConfig gcpConfig, String imageName);

  /**
   * Validates the Image
   *
   * @param gcpConfig
   */
  boolean verifyImageName(GcpInternalConfig gcpConfig, String imageName);

  /**
   * Validate the credentials
   *
   * @param gcpConfig
   * @return
   */
  boolean validateCredentials(GcpInternalConfig gcpConfig, String imageName);
}
