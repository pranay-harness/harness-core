package io.harness.states;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.steps.stepinfo.CleanupStepInfo;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.execution.status.Status;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.managerclient.ManagerCIResource;
import io.harness.network.SafeHttpCall;
import io.harness.references.SweepingOutputRefObject;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * State sends cleanup task to finish CI build job. It has to be executed in the end once all steps are complete
 */

@Slf4j
// TODO Cleanup Support for other types (Non K8)
public class CleanupStep implements Step, SyncExecutable {
  @Inject private ManagerCIResource managerCIResource;
  public static final StepType STEP_TYPE = CleanupStepInfo.typeInfo.getStepType();
  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;

  // TODO Async can not be supported at this point. We have to build polling framework on CI manager.
  //     Async will be supported once we will have delegate microservice ready.

  @Override
  public StepResponse executeSync(Ambiance ambiance, StepParameters stepParameters, StepInputPackage inputPackage,
      PassThroughData passThroughData) {
    try {
      K8PodDetails k8PodDetails = (K8PodDetails) executionSweepingOutputResolver.resolve(
          ambiance, SweepingOutputRefObject.builder().name(ContextElement.podDetails).build());

      final String namespace = k8PodDetails.getNamespace();
      final String clusterName = k8PodDetails.getClusterName();
      // TODO Pod cleanup is not require anymore after change in architecture, clean pvc and secrets instead of pod
      SafeHttpCall.execute(managerCIResource.podCleanupTask(clusterName, namespace, null));

      return StepResponse.builder().status(Status.SUCCEEDED).build();
    } catch (Exception e) {
      logger.error("state execution failed", e);
    }
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }
}
