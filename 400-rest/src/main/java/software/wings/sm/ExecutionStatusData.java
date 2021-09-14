/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.ExecutionStatusResponseData;

import lombok.Builder;
import lombok.Value;

/**
 * The type Execution status data.
 */
@OwnedBy(CDC)
@Value
@Builder
@TargetModule(HarnessModule._980_COMMONS)
public class ExecutionStatusData implements ExecutionStatusResponseData {
  private ExecutionStatus executionStatus;
}
