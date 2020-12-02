package io.harness;

import io.harness.pms.ambiance.Level;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.steps.StepType;

public class LevelUtils {
  public static Level buildLevelFromPlanNode(String runtimeId, PlanNodeProto node) {
    Level.Builder levelBuilder = Level.newBuilder()
                                     .setSetupId(node.getUuid())
                                     .setRuntimeId(runtimeId)
                                     .setIdentifier(node.getIdentifier())
                                     .setStepType(StepType.newBuilder().setType(node.getStepType().getType()).build());
    if (node.getGroup() != null) {
      levelBuilder.setGroup(node.getGroup());
    }
    return levelBuilder.build();
  }
}
