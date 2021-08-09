package io.harness.delegate.task.k8s;

import io.harness.delegate.beans.storeconfig.StoreDelegateConfig;

public interface ManifestDelegateConfig {
  ManifestType getManifestType();
  StoreDelegateConfig getStoreDelegateConfig();
}
