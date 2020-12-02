package io.harness.states;

import io.harness.beans.steps.stepinfo.RestoreCacheS3StepInfo;
import io.harness.pms.steps.StepType;

public class RestoreCacheS3Step extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = RestoreCacheS3StepInfo.typeInfo.getStepType();
}
