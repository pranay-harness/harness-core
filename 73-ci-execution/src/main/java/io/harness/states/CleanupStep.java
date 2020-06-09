package io.harness.states;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.steps.stepinfo.CleanupStepInfo;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.execution.status.Status;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.managerclient.ManagerCIResource;
import io.harness.network.SafeHttpCall;
import io.harness.references.SweepingOutputRefObject;
import io.harness.resolver.sweepingoutput.ExecutionSweepingOutputService;
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

@Slf4j
// TODO Cleanup Support for other types (Non K8)
public class CleanupStep implements Step, SyncExecutable {
  @Inject private ManagerCIResource managerCIResource;
  @Inject EngineExpressionService engineExpressionService;
  public static final StepType STEP_TYPE = CleanupStepInfo.typeInfo.getStepType();
  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;

  // TODO Async can not be supported at this point. We have to build polling framework on CI manager.
  //     Async will be supported once we will have delegate microservice ready.

  @Override
  public StepResponse executeSync(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs, PassThroughData passThroughData) {
    try {
      K8PodDetails k8PodDetails = (K8PodDetails) executionSweepingOutputResolver.resolve(
          ambiance, SweepingOutputRefObject.builder().name(ContextElement.podDetails).build());

      final String namespace = k8PodDetails.getNamespace();
      final String clusterName = k8PodDetails.getClusterName();
      final String podName = k8PodDetails.getPodName();
      // TODO Use k8 connector from element input, handle response
      SafeHttpCall.execute(managerCIResource.podCleanupTask(clusterName, namespace, podName));

      return StepResponse.builder().status(Status.SUCCEEDED).build();
    } catch (Exception e) {
      logger.error("state execution failed", e);
    }
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }
}
