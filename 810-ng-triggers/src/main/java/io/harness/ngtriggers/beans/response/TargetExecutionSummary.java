/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.beans.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TargetExecutionSummary {
  String triggerId;
  String targetId;
  String runtimeInput;
  String planExecutionId;
  String executionStatus;
  Long startTs;
}
