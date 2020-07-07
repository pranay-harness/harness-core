package io.harness.cdng.manifest.yaml;

import io.harness.yaml.core.intfc.OverridesApplier;
import io.harness.yaml.core.intfc.WithIdentifier;

import java.io.Serializable;

public interface ManifestAttributes extends WithIdentifier, OverridesApplier<ManifestAttributes>, Serializable {
  String getKind();
  default StoreConfig getStoreConfig() {
    return null;
  }
}
