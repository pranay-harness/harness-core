/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.k8s;

import io.harness.delegate.beans.storeconfig.StoreDelegateConfig;
import io.harness.delegate.task.helm.HelmCommandFlag;
import io.harness.k8s.model.HelmVersion;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HelmChartManifestDelegateConfig implements ManifestDelegateConfig {
  StoreDelegateConfig storeDelegateConfig;
  String chartName;
  String chartVersion;
  HelmVersion helmVersion;
  HelmCommandFlag helmCommandFlag;

  @Override
  public ManifestType getManifestType() {
    return ManifestType.HELM_CHART;
  }
}
