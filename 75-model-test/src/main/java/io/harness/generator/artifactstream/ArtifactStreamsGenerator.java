package io.harness.generator.artifactstream;

import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer;
import io.harness.generator.Randomizer.Seed;
import software.wings.beans.artifact.ArtifactStream;

public interface ArtifactStreamsGenerator {
  ArtifactStream ensureArtifact(Seed seed, Owners owners);
  ArtifactStream ensureArtifactStream(Randomizer.Seed seed, ArtifactStream artifactStream);
}
