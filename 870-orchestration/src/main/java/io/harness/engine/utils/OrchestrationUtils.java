package io.harness.engine.utils;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.plan.NodeType;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.execution.utils.AmbianceUtils;
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
    return nodeExecution.getNode().getStepCategory() == StepCategory.STAGE;
  }

  public static boolean isPipelineNode(NodeExecution nodeExecution) {
    return nodeExecution.getNode().getStepCategory() == StepCategory.PIPELINE;
  }

  public static NodeType currentNodeType(Ambiance ambiance) {
    Level level = AmbianceUtils.obtainCurrentLevel(ambiance);
    if (level == null) {
      return NodeType.PLAN;
    }
    // TODO: Remove this in next release
    if (isEmpty(level.getNodeType())) {
      return NodeType.PLAN_NODE;
    }
    return NodeType.valueOf(level.getNodeType());
  }
}
