package io.harness.steps.barriers.event;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.statusupdate.StepStatusUpdate;
import io.harness.engine.interrupts.statusupdate.StepStatusUpdateInfo;
import io.harness.execution.NodeExecution;
import io.harness.observer.AsyncInformObserver;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierPositionInfo.BarrierPosition.BarrierPositionType;
import io.harness.steps.barriers.service.BarrierService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.List;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class BarrierPositionHelperEventHandler implements AsyncInformObserver, StepStatusUpdate {
  @Inject @Named("PipelineExecutorService") ExecutorService executorService;
  @Inject NodeExecutionService nodeExecutionService;
  @Inject BarrierService barrierService;

  @Override
  public void onStepStatusUpdate(StepStatusUpdateInfo stepStatusUpdateInfo) {
    String planExecutionId = stepStatusUpdateInfo.getPlanExecutionId();
    try {
      NodeExecution nodeExecution = nodeExecutionService.get(stepStatusUpdateInfo.getNodeExecutionId());

      if (BarrierPositionType.STAGE.name().equals(nodeExecution.getNode().getGroup())) {
        updatePosition(planExecutionId, BarrierPositionType.STAGE, nodeExecution);
      } else if (BarrierPositionType.STEP_GROUP.name().equals(nodeExecution.getNode().getGroup())) {
        updatePosition(planExecutionId, BarrierPositionType.STEP_GROUP, nodeExecution);
      } else if (BarrierPositionType.STEP.name().equals(nodeExecution.getNode().getGroup())) {
        updatePosition(planExecutionId, BarrierPositionType.STEP, nodeExecution);
      }
    } catch (Exception e) {
      log.error("Failed to update barrier position for planExecutionId: [{}]", planExecutionId);
      throw e;
    }
  }

  private List<BarrierExecutionInstance> updatePosition(
      String planExecutionId, BarrierPositionType type, NodeExecution nodeExecution) {
    return barrierService.updatePosition(
        planExecutionId, type, nodeExecution.getNode().getUuid(), nodeExecution.getUuid());
  }

  @Override
  public ExecutorService getInformExecutorService() {
    return executorService;
  }
}
