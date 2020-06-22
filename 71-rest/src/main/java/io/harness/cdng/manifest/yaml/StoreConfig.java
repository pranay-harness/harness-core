package io.harness.cdng.manifest.yaml;

public interface StoreConfig {
  String getKind();
  StoreConfig cloneInternal();
}
