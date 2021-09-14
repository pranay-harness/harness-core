/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.config;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.Value;

@OwnedBy(CDC)
@Value
public class PipelineConfig {
  private boolean enabled;
  private Integer envStateTimeout;
  private Integer approvalStateTimeout;

  public PipelineConfig() {
    this.enabled = false;
    this.envStateTimeout = -1;
    this.approvalStateTimeout = -1;
  }
}
