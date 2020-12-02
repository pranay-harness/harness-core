package io.harness.states;

import io.harness.beans.steps.stepinfo.SaveCacheGCSStepInfo;
import io.harness.pms.steps.StepType;

public class SaveCacheGCSStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = SaveCacheGCSStepInfo.typeInfo.getStepType();
}
