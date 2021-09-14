/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.execution.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.event.handlers.SdkResponseProcessor;
import io.harness.execution.NodeExecution;
import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.events.base.PmsBaseEventHandler;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.registries.SdkResponseProcessorFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class SdkResponseHandler extends PmsBaseEventHandler<SdkResponseEventProto> {
  @Inject private SdkResponseProcessorFactory handlerRegistry;
  @Inject private NodeExecutionService nodeExecutionService;

  @Override
  protected Map<String, String> extraLogProperties(SdkResponseEventProto event) {
    return ImmutableMap.of("eventType", event.getSdkResponseEventType().name(), "nodeExecutionId",
        event.getNodeExecutionId(), "planExecutionId", event.getPlanExecutionId());
  }

  @Override
  protected Ambiance extractAmbiance(SdkResponseEventProto event) {
    return Ambiance.newBuilder().build();
  }

  @Override
  protected Map<String, String> extractMetricContext(Map<String, String> metadataMap, SdkResponseEventProto event) {
    return ImmutableMap.of("eventType", event.getSdkResponseEventType().name());
  }

  @Override
  protected String getMetricPrefix(SdkResponseEventProto message) {
    return "sdk_response_event";
  }

  @Override
  protected void handleEventWithContext(SdkResponseEventProto event) {
    SdkResponseProcessor handler = handlerRegistry.getHandler(event.getSdkResponseEventType());
    NodeExecution nodeExecution = nodeExecutionService.get(event.getNodeExecutionId());
    try (AutoLogContext ignore = AmbianceUtils.autoLogContext(nodeExecution.getAmbiance())) {
      log.info("Event for SdkResponseEvent received for eventType {}", event.getSdkResponseEventType());
      handler.handleEvent(event);
      log.info("Event for SdkResponseEvent for event type {} completed successfully", event.getSdkResponseEventType());
    }
  }
}
