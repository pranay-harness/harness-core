package io.harness.states;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.stepinfo.UploadToArtifactoryStepInfo;
import io.harness.pms.contracts.steps.StepType;

@OwnedBy(HarnessTeam.CI)
public class UploadToArtifactoryStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = UploadToArtifactoryStepInfo.STEP_TYPE;
}
