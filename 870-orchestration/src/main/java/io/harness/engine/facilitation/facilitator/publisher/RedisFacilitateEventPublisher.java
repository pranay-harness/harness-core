package io.harness.engine.facilitation.facilitator.publisher;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.events.PmsEventFrameworkConstants.SERVICE_NAME;

import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.utils.OrchestrationEventsFrameworkUtils;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.facilitators.FacilitatorEvent;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RedisFacilitateEventPublisher implements FacilitateEventPublisher {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEventsFrameworkUtils eventsFrameworkUtils;

  @Override
  public String publishEvent(String nodeExecutionId) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    FacilitatorEvent event = FacilitatorEvent.newBuilder()
                                 .setNodeExecutionId(nodeExecutionId)
                                 .setAmbiance(nodeExecution.getAmbiance())
                                 .setStepParameters(nodeExecution.getNode().getStepParametersBytes())
                                 .setStepType(nodeExecution.getNode().getStepType())
                                 .setNotifyId(generateUuid())
                                 .addAllRefObjects(nodeExecution.getNode().getRebObjectsList())
                                 .addAllFacilitatorObtainments(nodeExecution.getNode().getFacilitatorObtainmentsList())
                                 .build();

    Producer producer =
        eventsFrameworkUtils.obtainProducerForFacilitationEvent(nodeExecution.getNode().getServiceName());

    producer.send(Message.newBuilder()
                      .putAllMetadata(ImmutableMap.of(SERVICE_NAME, nodeExecution.getNode().getServiceName()))
                      .setData(event.toByteString())
                      .build());

    return event.getNotifyId();
  }
}
