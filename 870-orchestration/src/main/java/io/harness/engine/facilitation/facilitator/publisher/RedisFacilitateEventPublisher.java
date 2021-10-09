package io.harness.engine.facilitation.facilitator.publisher;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.pms.commons.events.PmsEventSender;
import io.harness.engine.pms.data.PmsTransputHelper;
import io.harness.execution.NodeExecution;
import io.harness.plan.PlanNode;
import io.harness.pms.contracts.facilitators.FacilitatorEvent;
import io.harness.pms.events.base.PmsEventCategory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RedisFacilitateEventPublisher implements FacilitateEventPublisher {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PmsEventSender eventSender;
  @Inject private PmsTransputHelper transputHelper;

  @Override
  public String publishEvent(String nodeExecutionId) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    PlanNode planNode = nodeExecution.getNode();
    FacilitatorEvent event = FacilitatorEvent.newBuilder()
                                 .setNodeExecutionId(nodeExecutionId)
                                 .setAmbiance(nodeExecution.getAmbiance())
                                 .setStepParameters(nodeExecution.getResolvedStepParametersBytes())
                                 .setStepType(planNode.getStepType())
                                 .setNotifyId(generateUuid())
                                 .addAllResolvedInput(transputHelper.resolveInputs(
                                     nodeExecution.getAmbiance(), planNode.getRefObjects()))
                                 .addAllFacilitatorObtainments(planNode.getFacilitatorObtainments())
                                 .build();

    String serviceName = planNode.getServiceName();
    return eventSender.sendEvent(
        nodeExecution.getAmbiance(), event.toByteString(), PmsEventCategory.FACILITATOR_EVENT, serviceName, true);
  }
}
