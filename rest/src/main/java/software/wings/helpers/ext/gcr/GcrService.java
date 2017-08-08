package software.wings.helpers.ext.gcr;

import software.wings.beans.GcpConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

/**
 * Created by brett on 8/2/17
 */
public interface GcrService {
  /**
   * Gets builds.
   *
   * @param gcpConfig      the gcp cloud provider config
   * @param maxNumberOfBuilds the max number of builds
   * @return the builds
   */
  List<BuildDetails> getBuilds(
      GcpConfig gcpConfig, ArtifactStreamAttributes artifactStreamAttributes, int maxNumberOfBuilds);

  /**
   * Gets last successful build.
   *
   * @param gcpConfig the gcr config
   * @return the last successful build
   */
  BuildDetails getLastSuccessfulBuild(GcpConfig gcpConfig, String imageName);

  /**
   * Validates the Image
   * @param gcpConfig
   */
  boolean verifyImageName(GcpConfig gcpConfig, ArtifactStreamAttributes artifactStreamAttributes);

  /**
   * Validate the credentials
   * @param gcpConfig
   * @return
   */
  boolean validateCredentials(GcpConfig gcpConfig, ArtifactStreamAttributes artifactStreamAttributes);
}
