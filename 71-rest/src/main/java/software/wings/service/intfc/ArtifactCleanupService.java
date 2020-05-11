package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.artifact.ArtifactStream;

@OwnedBy(CDC)
public interface ArtifactCleanupService {
  void cleanupArtifactsAsync(ArtifactStream artifactStream);
}
