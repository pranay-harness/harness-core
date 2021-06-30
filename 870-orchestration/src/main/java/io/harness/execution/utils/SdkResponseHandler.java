package io.harness.execution.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.event.handlers.SdkResponseProcessor;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.events.base.PmsBaseEventHandler;
import io.harness.registries.SdkResponseProcessorFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class SdkResponseHandler extends PmsBaseEventHandler<SdkResponseEventProto> {
  @Inject private SdkResponseProcessorFactory handlerRegistry;

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
  protected Map<String, String> extractMetricContext(SdkResponseEventProto event) {
    return ImmutableMap.of("eventType", event.getSdkResponseEventType().name());
  }

  @Override
  protected String getMetricPrefix(SdkResponseEventProto message) {
    return "sdk_response_event";
  }

  @Override
  protected void handleEventWithContext(SdkResponseEventProto event) {
    log.info("Event for SdkResponseEvent received with nodeExecutionId {} for eventType {}", event.getNodeExecutionId(),
        event.getSdkResponseEventType());
    SdkResponseProcessor handler = handlerRegistry.getHandler(event.getSdkResponseEventType());
    handler.handleEvent(event);
    log.info("Event for SdkResponseEvent with nodeExecutionId {} for event type {} completed successfully",
        event.getNodeExecutionId(), event.getSdkResponseEventType());
  }
}
