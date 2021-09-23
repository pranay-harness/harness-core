package io.harness.engine.utils;

import io.harness.plan.PlanNode;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.steps.StepType;

public class PmsLevelUtils {
  public static Level buildLevelFromPlanNode(String runtimeId, PlanNode node) {
    return buildLevelFromPlanNode(runtimeId, 0, node);
  }

  public static Level buildLevelFromPlanNode(String runtimeId, int retryIndex, PlanNode node) {
    Level.Builder levelBuilder = Level.newBuilder()
                                     .setSetupId(node.getUuid())
                                     .setRuntimeId(runtimeId)
                                     .setIdentifier(node.getIdentifier())
                                     .setRetryIndex(retryIndex)
                                     .setSkipExpressionChain(node.isSkipExpressionChain())
                                     .setStartTs(System.currentTimeMillis())
                                     .setStepType(StepType.newBuilder()
                                                      .setType(node.getStepType().getType())
                                                      .setStepCategory(node.getStepType().getStepCategory())
                                                      .build());
    if (node.getGroup() != null) {
      levelBuilder.setGroup(node.getGroup());
    }
    return levelBuilder.build();
  }
}
