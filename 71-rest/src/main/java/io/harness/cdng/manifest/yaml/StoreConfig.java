package io.harness.cdng.manifest.yaml;

import io.harness.yaml.core.intfc.OverridesApplier;

import java.io.Serializable;

public interface StoreConfig extends OverridesApplier<StoreConfig>, Serializable {
  String getKind();
  StoreConfig cloneInternal();
}
