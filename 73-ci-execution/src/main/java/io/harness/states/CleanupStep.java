package io.harness.states;

import static software.wings.common.CICommonPodConstants.CLUSTER_EXPRESSION;
import static software.wings.common.CICommonPodConstants.NAMESPACE_EXPRESSION;
import static software.wings.common.CICommonPodConstants.POD_NAME_EXPRESSION;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Produces;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.execution.status.Status;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.managerclient.ManagerCIResource;
import io.harness.network.SafeHttpCall;
import io.harness.state.Step;
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
  @Inject EngineExpressionService engineExpressionService;
  // TODO Async can not be supported at this point. We have to build polling framework on CI manager.
  //     Async will be supported once we will have delegate microservice ready.

  @Override
  public StepResponse executeSync(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs, PassThroughData passThroughData) {
    try {
      // TODO Use k8 connector from element input, handle response
      String clusterName = engineExpressionService.renderExpression(ambiance, CLUSTER_EXPRESSION);
      String namespace = engineExpressionService.renderExpression(ambiance, NAMESPACE_EXPRESSION);
      String podName = engineExpressionService.renderExpression(ambiance, POD_NAME_EXPRESSION);

      SafeHttpCall.execute(managerCIResource.podCleanupTask(clusterName, namespace, podName));

      return StepResponse.builder().status(Status.SUCCEEDED).build();
    } catch (Exception e) {
      logger.error("state execution failed", e);
    }
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }
}
