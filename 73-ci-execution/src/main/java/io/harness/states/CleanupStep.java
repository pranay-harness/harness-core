package io.harness.states;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Produces;
import io.harness.beans.steps.CleanupStepInfo;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.managerclient.ManagerCIResource;
import io.harness.network.SafeHttpCall;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * State sends cleanup task to finish CI build job. It has to be executed in the end once all steps are complete
 */

@Produces(Step.class)
@Slf4j
public class CleanupStep implements Step, SyncExecutable {
  @Inject private ManagerCIResource managerCIResource;

  // TODO Async can not be supported at this point. We have to build polling framework on CI manager.
  //     Async will be supported once we will have delegate microservice ready.

  @Override
  public StepResponse executeSync(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs, PassThroughData passThroughData) {
    try {
      // TODO Use k8 connector from element input, handle response

      SafeHttpCall.execute(managerCIResource.podCleanupTask("kubernetes_clusterqqq"));

      return StepResponse.builder().status(NodeExecutionStatus.SUCCEEDED).build();
    } catch (Exception e) {
      logger.error("state execution failed", e);
    }
    return StepResponse.builder().status(NodeExecutionStatus.SUCCEEDED).build();
  }

  @Override
  public StepType getType() {
    return CleanupStepInfo.stateType;
  }
}
