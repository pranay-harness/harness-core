/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.verification;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.ExecutionStatusResponseData;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class VerificationDataAnalysisResponse implements ExecutionStatusResponseData {
  private ExecutionStatus executionStatus;
  private VerificationStateAnalysisExecutionData stateExecutionData;
}
