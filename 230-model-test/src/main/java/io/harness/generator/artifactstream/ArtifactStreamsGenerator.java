package io.harness.generator.artifactstream;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer;
import io.harness.generator.Randomizer.Seed;

import software.wings.beans.artifact.ArtifactStream;

@OwnedBy(HarnessTeam.CDC)
public interface ArtifactStreamsGenerator {
  ArtifactStream ensureArtifactStream(Seed seed, Owners owners);
  ArtifactStream ensureArtifactStream(Seed seed, Owners owners, boolean atConnector);
  ArtifactStream ensureArtifactStream(Seed seed, Owners owners, boolean atConnector, boolean metadataOnly);
  ArtifactStream ensureArtifactStream(Randomizer.Seed seed, ArtifactStream artifactStream, Owners owners);
}
