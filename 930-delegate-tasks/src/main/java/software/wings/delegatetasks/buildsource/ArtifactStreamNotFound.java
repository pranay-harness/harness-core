package software.wings.delegatetasks.buildsource;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public class ArtifactStreamNotFound extends RuntimeException {
  public ArtifactStreamNotFound(String artifactStreamId) {
    super(String.format("ArtifactServer %s could not be found", artifactStreamId));
  }
}
