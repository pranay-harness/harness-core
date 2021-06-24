package io.harness.pms.execution.utils;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.grpc.utils.HTimestamps;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.StepType;

@OwnedBy(PIPELINE)
public class LevelUtils {
  public static Level buildLevelFromPlanNode(String runtimeId, PlanNodeProto node) {
    return buildLevelFromPlanNode(runtimeId, 0, node, System.currentTimeMillis());
  }

  public static Level buildLevelFromPlanNode(String runtimeId, PlanNodeProto node, long currLevelStartTs) {
    return buildLevelFromPlanNode(runtimeId, 0, node, currLevelStartTs);
  }

  public static Level buildLevelFromPlanNode(String runtimeId, int retryIndex, PlanNodeProto node) {
    return buildLevelFromPlanNode(runtimeId, retryIndex, node, System.currentTimeMillis());
  }

  public static Level buildLevelFromPlanNode(
      String runtimeId, int retryIndex, PlanNodeProto node, long currLevelStartTs) {
    Level.Builder levelBuilder = Level.newBuilder()
                                     .setSetupId(node.getUuid())
                                     .setRuntimeId(runtimeId)
                                     .setIdentifier(node.getIdentifier())
                                     .setRetryIndex(retryIndex)
                                     .setSkipExpressionChain(node.getSkipExpressionChain())
                                     .setStartTs(currLevelStartTs)
                                     .setStepType(StepType.newBuilder().setType(node.getStepType().getType()).build());
    if (node.getGroup() != null) {
      levelBuilder.setGroup(node.getGroup());
    }
    return levelBuilder.build();
  }
}
