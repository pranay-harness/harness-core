package io.harness.states;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Produces;
import io.harness.beans.steps.BuildEnvSetupStepInfo;
import io.harness.delegate.beans.ResponseData;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.managerclient.ManagerCIResource;
import io.harness.rest.RestResponse;
import io.harness.state.State;
import io.harness.state.StateType;
import io.harness.state.io.StateParameters;
import io.harness.state.io.StateResponse;
import io.harness.state.io.StateTransput;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

import java.util.List;

/**
 * This state will setup the build environment, clone the git repository for running CI job.
 */

@Produces(State.class)
@Slf4j
public class BuildEnvSetupState implements State, SyncExecutable {
  @Inject private ManagerCIResource managerCIResource;

  // TODO Async can not be supported at this point. We have to build polling framework on CI manager.
  //     Async will be supported once we will have delegate microservice ready.

  @Override
  public StateResponse executeSync(
      Ambiance ambiance, StateParameters parameters, List<StateTransput> inputs, PassThroughData passThroughData) {
    try {
      // Todo Pass params
      Response<RestResponse<ResponseData>> response = managerCIResource.createEnvTask().execute();

      return StateResponse.builder().status(NodeExecutionStatus.SUCCEEDED).build();
    } catch (Exception e) {
      logger.error("state execution failed", e);
    }
    return StateResponse.builder().status(NodeExecutionStatus.SUCCEEDED).build();
  }

  @Override
  public StateType getType() {
    return BuildEnvSetupStepInfo.stateType;
  }
}
