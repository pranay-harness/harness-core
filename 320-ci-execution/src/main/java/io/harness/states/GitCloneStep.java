package io.harness.states;

import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.Status;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.steps.StepType;

import lombok.extern.slf4j.Slf4j;

/**
 * This step does not do anything as checkout is happening in init container.
 * This step will be useful for non container execution
 */
@Slf4j
public class GitCloneStep implements SyncExecutable<GitCloneStepInfo> {
  public static final StepType STEP_TYPE = GitCloneStepInfo.typeInfo.getStepType();

  @Override
  public Class<GitCloneStepInfo> getStepParametersClass() {
    return GitCloneStepInfo.class;
  }

  @Override
  public StepResponse executeSync(Ambiance ambiance, GitCloneStepInfo stepParameters, StepInputPackage inputPackage,
      PassThroughData passThroughData) {
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }
}
