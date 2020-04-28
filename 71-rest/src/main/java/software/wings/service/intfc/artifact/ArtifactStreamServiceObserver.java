package software.wings.service.intfc.artifact;

import software.wings.beans.artifact.ArtifactStream;

import javax.validation.constraints.NotNull;

public interface ArtifactStreamServiceObserver {
  void onSaved(@NotNull ArtifactStream artifactStream);
  void onUpdated(@NotNull ArtifactStream currArtifactStream);
  void onDeleted(@NotNull ArtifactStream artifactStream);
}
