package io.harness.cdng.artifact.bean;

/**
 * Interface for getting Dto response to create concrete Artifact.
 */
public interface ArtifactAttributes extends Comparable<ArtifactAttributes> {
  ArtifactOutcome getArtifactOutcome(ArtifactConfigWrapper artifactConfig);
}
