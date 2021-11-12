package io.harness.engine.interrupts.statusupdate;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.observers.NodeStatusUpdateHandler;
import io.harness.engine.observers.NodeUpdateInfo;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.InterruptEffect;
import io.harness.pms.contracts.interrupts.InterruptType;

import com.google.inject.Inject;
import java.util.List;

@OwnedBy(PIPELINE)
public class TerminalStepStatusUpdate implements NodeStatusUpdateHandler {
  @Inject private InterruptService interruptService;

  @Override
  public void handleNodeStatusUpdate(NodeUpdateInfo nodeStatusUpdateInfo) {
    List<InterruptEffect> interruptEffects = nodeStatusUpdateInfo.getNodeExecution().getInterruptHistories();
    for (InterruptEffect interrupt : interruptEffects) {
      if (interrupt.getInterruptType() == InterruptType.RETRY) {
        interruptService.markProcessed(interrupt.getInterruptId(), Interrupt.State.PROCESSED_SUCCESSFULLY);
      }
    }
    //    Status planStatus = planExecutionService.calculateStatus(nodeStatusUpdateInfo.getPlanExecutionId());
    //    if (!StatusUtils.isFinalStatus(planStatus)) {
    //      planExecutionService.updateStatus(nodeStatusUpdateInfo.getPlanExecutionId(), planStatus);
    //    }
  }
}
