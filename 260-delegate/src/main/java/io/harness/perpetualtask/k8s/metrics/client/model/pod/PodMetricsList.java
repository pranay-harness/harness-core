/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.perpetualtask.k8s.metrics.client.model.pod;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.perpetualtask.k8s.metrics.client.model.common.CustomResourceList;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Singular;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class PodMetricsList extends CustomResourceList<PodMetrics> {
  @Builder
  public PodMetricsList(@Singular List<PodMetrics> items) {
    this.setKind("PodMetricsList");
    this.setItems(items);
  }
}
