package io.harness.delegate.task.k8s;

import io.harness.delegate.beans.storeconfig.StoreDelegateConfig;
import io.harness.k8s.model.HelmVersion;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HelmChartManifestDelegateConfig implements ManifestDelegateConfig {
  StoreDelegateConfig storeDelegateConfig;
  boolean skipResourceVersioning;
  HelmVersion helmVersion;

  @Override
  public ManifestType getManifestType() {
    return ManifestType.HELM_CHART;
  }
}
