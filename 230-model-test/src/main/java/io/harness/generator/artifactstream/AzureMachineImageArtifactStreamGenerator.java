package io.harness.generator.artifactstream;

import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import software.wings.beans.artifact.ArtifactStream;

public class AzureMachineImageArtifactStreamGenerator implements ArtifactStreamsGenerator {
  @Override
  public ArtifactStream ensureArtifactStream(Randomizer.Seed seed, OwnerManager.Owners owners) {
    return null;
  }

  @Override
  public ArtifactStream ensureArtifactStream(Randomizer.Seed seed, OwnerManager.Owners owners, boolean atConnector) {
    return null;
  }

  @Override
  public ArtifactStream ensureArtifactStream(
      Randomizer.Seed seed, OwnerManager.Owners owners, boolean atConnector, boolean metadataOnly) {
    return null;
  }

  @Override
  public ArtifactStream ensureArtifactStream(
      Randomizer.Seed seed, ArtifactStream artifactStream, OwnerManager.Owners owners) {
    return null;
  }
}
