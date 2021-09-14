/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.facilitation;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.facilitation.facilitator.CoreFacilitator;
import io.harness.engine.facilitation.facilitator.async.AsyncFacilitator;
import io.harness.engine.facilitation.facilitator.chain.ChildChainFacilitator;
import io.harness.engine.facilitation.facilitator.chain.TaskChainFacilitator;
import io.harness.engine.facilitation.facilitator.child.ChildFacilitator;
import io.harness.engine.facilitation.facilitator.chilidren.ChildrenFacilitator;
import io.harness.engine.facilitation.facilitator.publisher.FacilitateEventPublisher;
import io.harness.engine.facilitation.facilitator.sync.SyncFacilitator;
import io.harness.engine.facilitation.facilitator.task.TaskFacilitator;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.execution.OrchestrationFacilitatorType;

import com.google.inject.Inject;
import com.google.inject.Injector;

@OwnedBy(HarnessTeam.PIPELINE)
public class FacilitationHelper {
  @Inject private OrchestrationEngine orchestrationEngine;
  @Inject private FacilitateEventPublisher facilitateEventPublisher;
  @Inject Injector injector;

  public void facilitateExecution(NodeExecution nodeExecution) {
    if (customFacilitatorPresent(nodeExecution.getNode())) {
      facilitateEventPublisher.publishEvent(nodeExecution.getUuid());
      return;
    }

    PlanNodeProto node = nodeExecution.getNode();
    FacilitatorResponseProto currFacilitatorResponse = null;
    for (FacilitatorObtainment obtainment : node.getFacilitatorObtainmentsList()) {
      CoreFacilitator facilitator = getFacilitatorFromType(obtainment.getType());
      currFacilitatorResponse =
          facilitator.facilitate(nodeExecution.getAmbiance(), obtainment.getParameters().toByteArray());
      if (currFacilitatorResponse != null) {
        break;
      }
    }
    if (currFacilitatorResponse == null) {
      throw new InvalidRequestException("Cannot Determine Execution mode as facilitator Response is null");
    }
    orchestrationEngine.facilitateExecution(nodeExecution.getUuid(), currFacilitatorResponse);
  }

  private boolean customFacilitatorPresent(PlanNodeProto node) {
    if (isEmpty(node.getFacilitatorObtainmentsList())) {
      return true;
    }
    return !node.getFacilitatorObtainmentsList()
                .stream()
                .map(fo -> fo.getType().getType())
                .allMatch(OrchestrationFacilitatorType.ALL_FACILITATOR_TYPES::contains);
  }

  private CoreFacilitator getFacilitatorFromType(FacilitatorType type) {
    String fType = type.getType();
    switch (fType) {
      case OrchestrationFacilitatorType.ASYNC:
        return injector.getInstance(AsyncFacilitator.class);
      case OrchestrationFacilitatorType.SYNC:
        return injector.getInstance(SyncFacilitator.class);
      case OrchestrationFacilitatorType.TASK:
        return injector.getInstance(TaskFacilitator.class);
      case OrchestrationFacilitatorType.TASK_CHAIN:
        return injector.getInstance(TaskChainFacilitator.class);
      case OrchestrationFacilitatorType.CHILD:
        return injector.getInstance(ChildFacilitator.class);
      case OrchestrationFacilitatorType.CHILD_CHAIN:
        return injector.getInstance(ChildChainFacilitator.class);
      case OrchestrationFacilitatorType.CHILDREN:
        return injector.getInstance(ChildrenFacilitator.class);
      default:
        throw new InvalidRequestException("Core facilitator Type not found");
    }
  }
}
