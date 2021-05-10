package io.harness.steps.barriers.event;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.steps.barriers.BarrierSpecParameters;
import io.harness.steps.barriers.BarrierStep;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.service.BarrierService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class BarrierDropper implements AsyncOrchestrationEventHandler {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private BarrierService barrierService;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    String planExecutionId = event.getAmbiance().getPlanExecutionId();
    try {
      NodeExecutionProto nodeExecutionProto = event.getNodeExecutionProto();
      if (Status.ASYNC_WAITING != nodeExecutionProto.getStatus()
          || !BarrierStep.STEP_TYPE.equals(nodeExecutionProto.getNode().getStepType())) {
        return;
      }
      StepElementParameters stepElementParameters = RecastOrchestrationUtils.fromDocumentJson(
          nodeExecutionProto.getResolvedStepParameters(), StepElementParameters.class);
      BarrierSpecParameters barrierSpecParameters = (BarrierSpecParameters) stepElementParameters.getSpec();

      BarrierExecutionInstance barrierExecutionInstance =
          barrierService.findByIdentifierAndPlanExecutionId(barrierSpecParameters.getBarrierRef(), planExecutionId);
      barrierService.update(barrierExecutionInstance);
    } catch (Exception e) {
      log.error("[{}] event failed for plan [{}]", event.getEventType(), planExecutionId);
      throw e;
    }
  }
}
