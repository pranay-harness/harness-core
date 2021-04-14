package io.harness.steps.barriers.event;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierPositionInfo.BarrierPosition.BarrierPositionType;
import io.harness.steps.barriers.service.BarrierService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class BarrierPositionHelperEventHandler implements AsyncOrchestrationEventHandler {
  @Inject NodeExecutionService nodeExecutionService;
  @Inject BarrierService barrierService;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    String planExecutionId = event.getAmbiance().getPlanExecutionId();
    try {
      NodeExecution nodeExecution = nodeExecutionService.get(AmbianceUtils.obtainCurrentRuntimeId(event.getAmbiance()));

      if (BarrierPositionType.STAGE.name().equals(nodeExecution.getNode().getGroup())) {
        updatePosition(planExecutionId, BarrierPositionType.STAGE, nodeExecution);
      } else if (BarrierPositionType.STEP_GROUP.name().equals(nodeExecution.getNode().getGroup())) {
        updatePosition(planExecutionId, BarrierPositionType.STEP_GROUP, nodeExecution);
      } else if (BarrierPositionType.STEP.name().equals(nodeExecution.getNode().getGroup())) {
        updatePosition(planExecutionId, BarrierPositionType.STEP, nodeExecution);
      }
    } catch (Exception e) {
      log.error("[{}] event failed for plan [{}]", event.getEventType(), planExecutionId);
      throw e;
    }
  }

  private List<BarrierExecutionInstance> updatePosition(
      String planExecutionId, BarrierPositionType type, NodeExecution nodeExecution) {
    return barrierService.updatePosition(
        planExecutionId, type, nodeExecution.getNode().getUuid(), nodeExecution.getUuid());
  }
}
