package software.wings.service.intfc;

import software.wings.beans.artifact.ArtifactStream;

public interface ArtifactCleanupService { void cleanupArtifactsAsync(String appId, ArtifactStream artifactStream); }
