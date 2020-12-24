package io.harness.states;

import io.harness.beans.steps.stepinfo.UploadToGCSStepInfo;
import io.harness.pms.contracts.steps.StepType;

public class UploadToGCSStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = UploadToGCSStepInfo.STEP_TYPE;
}
