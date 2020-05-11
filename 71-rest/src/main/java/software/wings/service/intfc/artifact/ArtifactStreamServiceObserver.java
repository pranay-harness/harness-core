package software.wings.service.intfc.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.artifact.ArtifactStream;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
public interface ArtifactStreamServiceObserver {
  void onSaved(@NotNull ArtifactStream artifactStream);
  void onUpdated(@NotNull ArtifactStream currArtifactStream);
  void onDeleted(@NotNull ArtifactStream artifactStream);
}
