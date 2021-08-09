package io.harness.delegate.task.k8s;

import io.harness.delegate.beans.storeconfig.StoreDelegateConfig;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OpenshiftManifestDelegateConfig implements ManifestDelegateConfig {
  StoreDelegateConfig storeDelegateConfig;

  @Override
  public ManifestType getManifestType() {
    return ManifestType.OPENSHIFT_TEMPLATE;
  }
}
