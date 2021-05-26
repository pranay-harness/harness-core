package io.harness.engine.interrupts.handlers.publisher;

import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.InterruptEventQueuePublisher;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecutionMapper;
import io.harness.interrupts.Interrupt;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.interrupts.InterruptEvent;

import com.google.inject.Inject;

public class MongoInterruptEventPublisher implements InterruptEventPublisher {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private InterruptEventQueuePublisher interruptEventQueuePublisher;

  @Override
  public String publishEvent(String nodeExecutionId, Interrupt interrupt, InterruptType interruptType) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    InterruptEvent interruptEvent = InterruptEvent.builder()
                                        .interruptUuid(interrupt.getUuid())
                                        .nodeExecution(NodeExecutionMapper.toNodeExecutionProto(nodeExecution))
                                        .interruptType(interruptType)
                                        .metadata(interrupt.getMetadata())
                                        .build();
    interruptEventQueuePublisher.send(nodeExecution.getNode().getServiceName(), interruptEvent);
    return interruptEvent.getNotifyId();
  }
}
