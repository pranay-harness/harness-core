package io.harness.engine.interrupts.handlers.publisher;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.events.PmsEventFrameworkConstants.INTERRUPT_PRODUCER;
import static io.harness.pms.events.PmsEventFrameworkConstants.SERVICE_NAME;

import io.harness.data.structure.CollectionUtils;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecutionMapper;
import io.harness.interrupts.Interrupt;
import io.harness.pms.contracts.interrupts.InterruptEvent;
import io.harness.pms.contracts.interrupts.InterruptType;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class RedisInterruptEventPublisher implements InterruptEventPublisher {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject @Named(INTERRUPT_PRODUCER) private Producer eventProducer;

  @Override
  public String publishEvent(String nodeExecutionId, Interrupt interrupt, InterruptType interruptType) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    InterruptEvent interruptEvent = InterruptEvent.newBuilder()
                                        .setInterruptUuid(interrupt.getUuid())
                                        .setNodeExecution(NodeExecutionMapper.toNodeExecutionProto(nodeExecution))
                                        .setType(interruptType)
                                        .putAllMetadata(CollectionUtils.emptyIfNull(interrupt.getMetadata()))
                                        .setNotifyId(generateUuid())
                                        .build();
    eventProducer.send(Message.newBuilder()
                           .putAllMetadata(ImmutableMap.of(SERVICE_NAME, nodeExecution.getNode().getServiceName()))
                           .setData(interruptEvent.toByteString())
                           .build());
    return interruptEvent.getNotifyId();
  }
}
