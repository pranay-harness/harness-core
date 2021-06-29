package io.harness.engine.interrupts.consumers;

import static io.harness.govern.Switch.noop;
import static io.harness.interrupts.Interrupt.State.PROCESSED_SUCCESSFULLY;
import static io.harness.interrupts.Interrupt.State.PROCESSED_UNSUCCESSFULLY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.interrupts.helpers.AbortHelper;
import io.harness.execution.NodeExecution;
import io.harness.interrupts.InterruptEffect;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.interrupts.InterruptEventResponse;
import io.harness.pms.events.base.PmsBaseEventHandler;
import io.harness.tasks.BinaryResponseData;
import io.harness.waiter.WaitNotifyEngine;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class InterruptResponseHandler extends PmsBaseEventHandler<InterruptEventResponse> {
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private AbortHelper abortHelper;
  @Inject private InterruptService interruptService;
  @Inject private OrchestrationEngine orchestrationEngine;

  @Override
  protected Map<String, String> extraLogProperties(InterruptEventResponse event) {
    return ImmutableMap.<String, String>builder()
        .put("interruptType", event.getInterruptType().name())
        .put("notifyId", event.getNotifyId())
        .build();
  }

  @Override
  protected Ambiance extractAmbiance(InterruptEventResponse event) {
    return Ambiance.newBuilder().build();
  }

  @Override
  protected Map<String, String> extractMetricContext(InterruptEventResponse event) {
    return ImmutableMap.of();
  }

  @Override
  protected String getMetricPrefix(InterruptEventResponse message) {
    return null;
  }

  @Override
  protected void handleEventWithContext(InterruptEventResponse event) {
    switch (event.getInterruptType()) {
      case ABORT:
        handleAbortInterruptResponse(event);
        return;
      case CUSTOM_FAILURE:
        handleCustomFailureInterruptResponse(event);
        return;
      default:
        log.warn("No Handling present for Interrupt Event Notify of type : {}", event.getInterruptType());
        noop();
    }
  }

  private void handleAbortInterruptResponse(InterruptEventResponse eventResponse) {
    NodeExecution nodeExecution = nodeExecutionService.get(eventResponse.getNodeExecutionId());
    abortHelper.abortDiscontinuingNode(
        nodeExecution, eventResponse.getInterruptId(), eventResponse.getInterruptConfig());
    waitNotifyEngine.doneWith(eventResponse.getNodeExecutionId() + "|" + eventResponse.getInterruptId(),
        BinaryResponseData.builder().build());
    log.info(
        "[PIPELINE] Handled InterruptEvent Notify Successfully for type - " + eventResponse.getInterruptType().name());
  }

  private void handleCustomFailureInterruptResponse(InterruptEventResponse eventResponse) {
    try {
      NodeExecution updatedNodeExecution = nodeExecutionService.update(eventResponse.getNodeExecutionId(), ops -> {
        ops.set(NodeExecution.NodeExecutionKeys.endTs, System.currentTimeMillis());
        ops.addToSet(NodeExecution.NodeExecutionKeys.interruptHistories,
            InterruptEffect.builder()
                .interruptId(eventResponse.getInterruptId())
                .tookEffectAt(System.currentTimeMillis())
                .interruptType(eventResponse.getInterruptType())
                .interruptConfig(eventResponse.getInterruptConfig())
                .build());
      });
      orchestrationEngine.concludeNodeExecution(updatedNodeExecution, Status.FAILED);
    } catch (Exception ex) {
      interruptService.markProcessed(eventResponse.getInterruptId(), PROCESSED_UNSUCCESSFULLY);
      throw ex;
    }
    interruptService.markProcessed(eventResponse.getInterruptId(), PROCESSED_SUCCESSFULLY);
    log.info(
        "[PIPELINE] Handled InterruptEvent Notify Successfully for type - " + eventResponse.getInterruptType().name());
  }
}
