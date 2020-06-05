package io.harness.cdng.artifact.bean.yaml;

import io.harness.cdng.artifact.bean.artifactsource.ArtifactSourceAttributes;

/**
 * This class is used for YAML spec.
 */
public interface Spec {
  // TODO(archit): whether case sensitive or not.
  String getUniqueHash();

  ArtifactSourceAttributes getSourceAttributes();
}
