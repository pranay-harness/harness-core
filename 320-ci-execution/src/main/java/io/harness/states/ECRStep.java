package io.harness.states;

import io.harness.beans.steps.stepinfo.ECRStepInfo;
import io.harness.pms.contracts.steps.StepType;

public class ECRStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = ECRStepInfo.typeInfo.getStepType();
}
