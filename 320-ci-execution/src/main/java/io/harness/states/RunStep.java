package io.harness.states;

import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.pms.contracts.steps.StepType;

public class RunStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = RunStepInfo.typeInfo.getStepType();
}
