/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.utils;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.execution.utils.StatusUtils;

import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class OrchestrationUtils {
  public Status calculateStatus(List<NodeExecution> nodeExecutions, String planExecutionId) {
    List<Status> statuses = nodeExecutions.stream().map(NodeExecution::getStatus).collect(Collectors.toList());
    return StatusUtils.calculateStatus(statuses, planExecutionId);
  }

  public static boolean isStageNode(NodeExecution nodeExecution) {
    return nodeExecution.getNode().getStepType().getStepCategory() == StepCategory.STAGE;
  }

  public static boolean isPipelineNode(NodeExecution nodeExecution) {
    return nodeExecution.getNode().getStepType().getStepCategory() == StepCategory.PIPELINE;
  }
}
