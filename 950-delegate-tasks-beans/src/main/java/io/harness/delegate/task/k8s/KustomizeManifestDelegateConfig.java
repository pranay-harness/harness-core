package io.harness.delegate.task.k8s;

import io.harness.delegate.beans.storeconfig.StoreDelegateConfig;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class KustomizeManifestDelegateConfig implements ManifestDelegateConfig {
  StoreDelegateConfig storeDelegateConfig;
  String pluginPath;
  String kustomizeDirPath;

  @Override
  public ManifestType getManifestType() {
    return ManifestType.KUSTOMIZE;
  }
}
