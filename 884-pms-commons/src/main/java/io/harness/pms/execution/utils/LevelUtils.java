package io.harness.pms.execution.utils;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.steps.StepCategory;

@OwnedBy(PIPELINE)
public class LevelUtils {
  public static boolean isStageLevel(Level level) {
    return level.getStepType().getStepCategory() == StepCategory.STAGE;
  }
}
