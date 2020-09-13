package io.harness.states;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.execution.status.Status;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.managerclient.ManagerCIResource;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.stateutils.buildstate.BuildSetupUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * This step does not do anything as checkout is happening in init container.
 * This step will be useful for non container execution
 */
@Slf4j
public class GitCloneStep implements Step, SyncExecutable {
  @Inject private ManagerCIResource managerCIResource;
  @Inject private BuildSetupUtils buildSetupUtils;
  public static final StepType STEP_TYPE = GitCloneStepInfo.typeInfo.getStepType();
  @Override
  public StepResponse executeSync(Ambiance ambiance, StepParameters stepParameters, StepInputPackage inputPackage,
      PassThroughData passThroughData) {
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }
}