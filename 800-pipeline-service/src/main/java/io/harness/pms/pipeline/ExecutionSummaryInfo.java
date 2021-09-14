/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.pipeline;

import io.harness.pms.execution.ExecutionStatus;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExecutionSummaryInfo {
  long lastExecutionTs;
  ExecutionStatus lastExecutionStatus;
  @Builder.Default Map<String, Integer> numOfErrors = new HashMap<>(); // total number of errors in the last ten days
  @Builder.Default
  Map<String, Integer> deployments =
      new HashMap<>(); // no of deployments for each of the last 10 days, most recent first
  String lastExecutionId;
}
