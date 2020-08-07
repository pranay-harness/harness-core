package io.harness.delegate.task.k8s;

import io.harness.delegate.beans.storeconfig.StoreDelegateConfig;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sManifestDelegateConfig implements ManifestDelegateConfig {
  StoreDelegateConfig storeDelegateConfig;
}
