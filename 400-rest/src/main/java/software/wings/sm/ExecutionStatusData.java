/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
