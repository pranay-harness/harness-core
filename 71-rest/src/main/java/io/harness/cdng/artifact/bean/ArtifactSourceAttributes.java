package io.harness.cdng.artifact.bean;

import io.harness.cdng.artifact.delegate.DelegateArtifactService;

/**
 * Interface of DTO to be passed to Delegate Tasks.
 */
public interface ArtifactSourceAttributes {
  Class<? extends DelegateArtifactService> getDelegateArtifactServiceClass();
}
