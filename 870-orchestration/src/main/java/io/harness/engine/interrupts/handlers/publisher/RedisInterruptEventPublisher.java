package io.harness.engine.interrupts.handlers.publisher;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.events.PmsEventFrameworkConstants.SERVICE_NAME;

import io.harness.data.structure.CollectionUtils;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.utils.OrchestrationEventsFrameworkUtils;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecutionMapper;
import io.harness.interrupts.Interrupt;
import io.harness.pms.contracts.interrupts.InterruptEvent;
import io.harness.pms.contracts.interrupts.InterruptType;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

public class RedisInterruptEventPublisher implements InterruptEventPublisher {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEventsFrameworkUtils eventsFrameworkUtils;

  @Override
  public String publishEvent(String nodeExecutionId, Interrupt interrupt, InterruptType interruptType) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    String serviceName = nodeExecution.getNode().getServiceName();
    Producer producer = eventsFrameworkUtils.obtainProducerForInterrupt(serviceName);
    InterruptEvent interruptEvent = InterruptEvent.newBuilder()
                                        .setInterruptUuid(interrupt.getUuid())
                                        .setNodeExecution(NodeExecutionMapper.toNodeExecutionProto(nodeExecution))
                                        .setType(interruptType)
                                        .putAllMetadata(CollectionUtils.emptyIfNull(interrupt.getMetadata()))
                                        .setNotifyId(generateUuid())
                                        .build();
    producer.send(Message.newBuilder()
                      .putAllMetadata(ImmutableMap.of(SERVICE_NAME, nodeExecution.getNode().getServiceName()))
                      .setData(interruptEvent.toByteString())
                      .build());
    return interruptEvent.getNotifyId();
  }
}
