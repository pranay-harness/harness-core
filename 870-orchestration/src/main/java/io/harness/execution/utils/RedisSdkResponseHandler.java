package io.harness.execution.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.event.handlers.SdkResponseEventHandler;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.sdk.core.execution.events.NodeBaseEventHandler;
import io.harness.registries.SdkNodeExecutionEventHandlerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class RedisSdkResponseHandler extends NodeBaseEventHandler<SdkResponseEventProto> {
  @Inject private SdkNodeExecutionEventHandlerFactory handlerRegistry;

  @Override
  protected Map<String, String> extraLogProperties(SdkResponseEventProto event) {
    return ImmutableMap.of("eventType", event.getSdkResponseEventType().name());
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
  protected boolean handleEventWithContext(SdkResponseEventProto event) {
    log.info("Event for SdkResponseEvent received");
    SdkResponseEventHandler handler = handlerRegistry.getHandler(event.getSdkResponseEventType());
    handler.handleEvent(event);
    return true;
  }
}
