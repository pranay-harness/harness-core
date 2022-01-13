/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
