package io.harness.engine.interrupts.handlers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.interrupts.Interrupt.State.PROCESSED_UNSUCCESSFULLY;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionUpdateFailedException;
import io.harness.engine.interrupts.InterruptEventQueuePublisher;
import io.harness.engine.interrupts.InterruptProcessingFailedException;
import io.harness.engine.interrupts.callback.FailureInterruptCallback;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecutionMapper;
import io.harness.interrupts.Interrupt;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.interrupts.InterruptEvent;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Slf4j
public class CustomFailureInterruptHandler extends MarkStatusInterruptHandler {
  @Inject private InterruptEventQueuePublisher interruptEventQueuePublisher;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;

  @Override
  public Interrupt handleInterrupt(Interrupt interrupt) {
    NodeExecution nodeExecution = nodeExecutionService.get(interrupt.getNodeExecutionId());
    try {
      Interrupt updatedInterrupt = interruptService.markProcessing(interrupt.getUuid());
      InterruptEvent interruptEvent = InterruptEvent.builder()
                                          .interruptUuid(interrupt.getUuid())
                                          .nodeExecution(NodeExecutionMapper.toNodeExecutionProto(nodeExecution))
                                          .interruptType(InterruptType.CUSTOM_FAILURE)
                                          .metadata(interrupt.getMetadata())
                                          .build();
      interruptEventQueuePublisher.send(nodeExecution.getNode().getServiceName(), interruptEvent);

      waitNotifyEngine.waitForAllOn(publisherName,
          FailureInterruptCallback.builder()
              .nodeExecutionId(nodeExecution.getUuid())
              .interruptId(interrupt.getUuid())
              .interruptType(interrupt.getType())
              .interruptConfig(interrupt.getInterruptConfig())
              .build(),
          interruptEvent.getNotifyId());
      return updatedInterrupt;
    } catch (NodeExecutionUpdateFailedException ex) {
      interruptService.markProcessed(interrupt.getUuid(), PROCESSED_UNSUCCESSFULLY);
      throw new InterruptProcessingFailedException(InterruptType.CUSTOM_FAILURE,
          "Custom Failure Interrupt failed for execution Plan :" + nodeExecution.getAmbiance().getPlanExecutionId()
              + "for NodeExecutionId: " + nodeExecution.getUuid(),
          ex);
    } catch (Exception e) {
      interruptService.markProcessed(interrupt.getUuid(), PROCESSED_UNSUCCESSFULLY);
      log.error("Error in handling Custom Failure Interrupt", e);
      throw new InvalidRequestException("Error in handling Custom Failure Interrupt, " + e.getMessage());
    }
  }
}
