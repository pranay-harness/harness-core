package io.harness.states;

import io.harness.beans.steps.stepinfo.RestoreCacheStepInfo;
import io.harness.pms.steps.StepType;

public class RestoreCacheStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = RestoreCacheStepInfo.typeInfo.getStepType();
}
