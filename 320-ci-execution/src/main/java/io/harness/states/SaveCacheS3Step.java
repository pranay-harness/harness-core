package io.harness.states;

import io.harness.beans.steps.stepinfo.SaveCacheS3StepInfo;
import io.harness.pms.contracts.steps.StepType;

public class SaveCacheS3Step extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = SaveCacheS3StepInfo.typeInfo.getStepType();
}
