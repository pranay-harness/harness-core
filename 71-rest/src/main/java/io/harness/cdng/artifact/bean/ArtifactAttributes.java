package io.harness.cdng.artifact.bean;

/**
 * Interface for getting Dto response to create concrete Artifact.
 */
public interface ArtifactAttributes {
  Artifact getArtifact(String accountId, String artifactStreamUUID, String sourceType);
}
