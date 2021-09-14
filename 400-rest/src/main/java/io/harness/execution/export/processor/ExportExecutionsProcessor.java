/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.execution.export.processor;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.export.metadata.ExecutionMetadata;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
public interface ExportExecutionsProcessor {
  void visitExecutionMetadata(@NotNull ExecutionMetadata executionMetadata);
  void process();
}
