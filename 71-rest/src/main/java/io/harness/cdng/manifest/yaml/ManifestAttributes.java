package io.harness.cdng.manifest.yaml;

import io.harness.yaml.core.intfc.WithIdentifier;

public interface ManifestAttributes extends WithIdentifier {
  String getKind();
  default StoreConfig getStoreConfig() {
    return null;
  }
}
