/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.facilitation.facilitator.publisher;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.pms.commons.events.PmsEventSender;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.facilitators.FacilitatorEvent;
import io.harness.pms.events.base.PmsEventCategory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RedisFacilitateEventPublisher implements FacilitateEventPublisher {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PmsEventSender eventSender;

  @Override
  public String publishEvent(String nodeExecutionId) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    FacilitatorEvent event = FacilitatorEvent.newBuilder()
                                 .setNodeExecutionId(nodeExecutionId)
                                 .setAmbiance(nodeExecution.getAmbiance())
                                 .setStepParameters(nodeExecution.getResolvedStepParametersBytes())
                                 .setStepType(nodeExecution.getNode().getStepType())
                                 .setNotifyId(generateUuid())
                                 .addAllRefObjects(nodeExecution.getNode().getRebObjectsList())
                                 .addAllFacilitatorObtainments(nodeExecution.getNode().getFacilitatorObtainmentsList())
                                 .build();

    String serviceName = nodeExecution.getNode().getServiceName();
    return eventSender.sendEvent(
        nodeExecution.getAmbiance(), event.toByteString(), PmsEventCategory.FACILITATOR_EVENT, serviceName, true);
  }
}
