package io.harness.monitoring;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;

import io.harness.metrics.ThreadAutoLogContext;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.serializer.ProtoUtils;

import java.util.HashMap;
import java.util.Map;

public class SdkResponseEventMessageExtractor implements MonitoringMetadataExtractor<SdkResponseEventProto> {
  @Override
  public ThreadAutoLogContext metricContext(SdkResponseEventProto sdkResponseEvent) {
    Map<String, String> logContext = new HashMap<>();
    logContext.put("eventType", sdkResponseEvent.getSdkResponseEventType().name());
    return new ThreadAutoLogContext(logContext, OVERRIDE_NESTS);
  }

  @Override
  public String getMetricPrefix(SdkResponseEventProto message) {
    return "sdk_response_event";
  }

  @Override
  public Class<SdkResponseEventProto> getType() {
    return SdkResponseEventProto.class;
  }

  @Override
  public Long getCreatedAt(SdkResponseEventProto message) {
    return ProtoUtils.timestampToUnixMillis(message.getCreatedAt());
  }
}
