package io.harness.states;

import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * This step does not do anything as checkout is happening in init container.
 * This step will be useful for non container execution
 */
@Slf4j
public class GitCloneStep implements SyncExecutable<GitCloneStepInfo> {
  public static final StepType STEP_TYPE = GitCloneStepInfo.STEP_TYPE;

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
