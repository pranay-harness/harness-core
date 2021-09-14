/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.execution.utils;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;

@OwnedBy(PIPELINE)
public class LevelUtils {
  public static Level buildLevelFromPlanNode(String runtimeId, PlanNodeProto node) {
    return buildLevelFromPlanNode(runtimeId, 0, node);
  }

  public static Level buildLevelFromPlanNode(String runtimeId, int retryIndex, PlanNodeProto node) {
    Level.Builder levelBuilder = Level.newBuilder()
                                     .setSetupId(node.getUuid())
                                     .setRuntimeId(runtimeId)
                                     .setIdentifier(node.getIdentifier())
                                     .setRetryIndex(retryIndex)
                                     .setSkipExpressionChain(node.getSkipExpressionChain())
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

  public static boolean isStageLevel(Level level) {
    return level.getStepType().getStepCategory() == StepCategory.STAGE;
  }
}
