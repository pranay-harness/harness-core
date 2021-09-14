/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.progress.publisher;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.pms.commons.events.PmsEventSender;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.progress.ProgressEvent;
import io.harness.pms.events.base.PmsEventCategory;
import io.harness.tasks.BinaryResponseData;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class RedisProgressEventPublisher implements ProgressEventPublisher {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PmsEventSender eventSender;

  @Override
  public String publishEvent(String nodeExecutionId, BinaryResponseData progressData) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    String serviceName = nodeExecution.getNode().getServiceName();
    ProgressEvent progressEvent = ProgressEvent.newBuilder()
                                      .setAmbiance(nodeExecution.getAmbiance())
                                      .setExecutionMode(nodeExecution.getMode())
                                      .setStepParameters(nodeExecution.getResolvedStepParametersBytes())
                                      .setProgressBytes(ByteString.copyFrom(progressData.getData()))
                                      .build();

    return eventSender.sendEvent(
        nodeExecution.getAmbiance(), progressEvent.toByteString(), PmsEventCategory.PROGRESS_EVENT, serviceName, false);
  }
}
