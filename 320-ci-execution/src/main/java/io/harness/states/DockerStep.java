package io.harness.states;

import io.harness.beans.steps.stepinfo.DockerStepInfo;
import io.harness.pms.contracts.steps.StepType;

public class DockerStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = DockerStepInfo.typeInfo.getStepType();
}
