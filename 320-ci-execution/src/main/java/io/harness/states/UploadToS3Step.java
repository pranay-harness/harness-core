package io.harness.states;

import io.harness.beans.steps.stepinfo.UploadToS3StepInfo;
import io.harness.pms.steps.StepType;

public class UploadToS3Step extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = UploadToS3StepInfo.typeInfo.getStepType();
}
