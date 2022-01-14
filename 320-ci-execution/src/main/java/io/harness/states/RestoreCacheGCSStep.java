package io.harness.states;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.stepinfo.RestoreCacheGCSStepInfo;
import io.harness.pms.contracts.steps.StepType;

@OwnedBy(HarnessTeam.CI)
public class RestoreCacheGCSStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = RestoreCacheGCSStepInfo.STEP_TYPE;
}
