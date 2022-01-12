/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
