package io.harness.states;

import io.harness.beans.steps.stepinfo.GCRStepInfo;
import io.harness.pms.contracts.steps.StepType;

public class GCRStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = GCRStepInfo.typeInfo.getStepType();
}
