/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.aggregator;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.metrics.AutoMetricContext;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@OwnedBy(PL)
public class AggregatorMetricsContext extends AutoMetricContext {
  public AggregatorMetricsContext(String namespace, String containerName) {
    put("namespace", namespace);
    put("containerName", containerName);
  }
}
