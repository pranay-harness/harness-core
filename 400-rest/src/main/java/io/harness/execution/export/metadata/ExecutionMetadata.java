/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface ExecutionMetadata extends GraphNodeVisitable {
  String getId();
  String getExecutionType();
  String getAppId();
  String getApplication();
  String getEntityName();
  TimingMetadata getTiming();
}
