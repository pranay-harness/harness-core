/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.pms.advise.publisher;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.pms.commons.events.PmsEventSender;
import io.harness.execution.NodeExecution;
import io.harness.interrupts.InterruptEffect;
import io.harness.pms.contracts.advisers.AdviseEvent;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.events.base.PmsEventCategory;

import com.google.inject.Inject;
import java.util.List;

@OwnedBy(HarnessTeam.PIPELINE)
public class RedisNodeAdviseEventPublisher implements NodeAdviseEventPublisher {
  @Inject NodeExecutionService nodeExecutionService;
  @Inject private PmsEventSender eventSender;

  @Override
  public String publishEvent(String nodeExecutionId, Status fromStatus) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    String serviceName = nodeExecution.getNode().getServiceName();
    AdviseEvent adviseEvent =
        AdviseEvent.newBuilder()
            .setAmbiance(nodeExecution.getAmbiance())
            .setFailureInfo(nodeExecution.getFailureInfo())
            .addAllAdviserObtainments(nodeExecution.getNode().getAdviserObtainmentsList())
            .setIsPreviousAdviserExpired(isPreviousAdviserExpired(nodeExecution.getInterruptHistories()))
            .addAllRetryIds(nodeExecution.getRetryIds())
            .setNotifyId(generateUuid())
            .setToStatus(nodeExecution.getStatus())
            .setFromStatus(fromStatus)
            .build();

    return eventSender.sendEvent(
        nodeExecution.getAmbiance(), adviseEvent.toByteString(), PmsEventCategory.NODE_ADVISE, serviceName, true);
  }

  private boolean isPreviousAdviserExpired(List<InterruptEffect> interruptHistories) {
    if (interruptHistories.size() == 0) {
      return false;
    }
    return interruptHistories.get(interruptHistories.size() - 1).getInterruptConfig().getIssuedBy().hasTimeoutIssuer();
  }
}
