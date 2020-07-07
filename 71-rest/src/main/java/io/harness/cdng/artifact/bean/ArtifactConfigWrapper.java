package io.harness.cdng.artifact.bean;

import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;
import io.harness.yaml.core.intfc.OverridesApplier;
import io.harness.yaml.core.intfc.WithIdentifier;

/**
 * wrapper object for dockerhub, gcr, etc element.
 * artifacts:
 *      primary:
 *             dockerhub:
 *      sidecars
 *          -sidecar:
 *              identifier:
 *              dockerhub:
 */

public interface ArtifactConfigWrapper extends WithIdentifier, OverridesApplier<ArtifactConfigWrapper> {
  String getSourceType();
  String getUniqueHash();
  ArtifactSource getArtifactSource(String accountId);
  ArtifactSourceAttributes getSourceAttributes();
  String getArtifactType();
  String setArtifactType(String artifactType);
  String setIdentifier(String identifier);
}
