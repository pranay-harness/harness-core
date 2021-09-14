/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event;

import io.harness.execution.NodeExecution;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GraphNodeUpdateInfo {
  String planExecutionId;
  List<NodeExecution> nodeExecutions;
}
