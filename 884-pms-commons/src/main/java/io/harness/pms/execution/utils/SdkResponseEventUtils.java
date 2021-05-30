package io.harness.pms.execution.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.logging.AutoLogContext.OverrideBehavior;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.execution.SdkResponseEvent;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class SdkResponseEventUtils {
  public AutoLogContext obtainLogContext(SdkResponseEvent sdkResponseEvent) {
    return new AutoLogContext(logContextMap(sdkResponseEvent), OverrideBehavior.OVERRIDE_NESTS);
  }

  private Map<String, String> logContextMap(SdkResponseEvent sdkResponseEvent) {
    Map<String, String> logContext = new HashMap<>();
    logContext.put("sdkResponseEventType", sdkResponseEvent.getSdkResponseEventType().name());
    logContext.put("nodeExecutionId", sdkResponseEvent.getSdkResponseEventRequest().getNodeExecutionId());
    return logContext;
  }

  public SdkResponseEvent fromProtoToSdkResponseEvent(SdkResponseEventProto sdkResponseEventProto) {
    return SdkResponseEvent.builder()
        .sdkResponseEventRequest(sdkResponseEventProto.getSdkResponseEventRequest())
        .sdkResponseEventType(sdkResponseEventProto.getSdkResponseEventType())
        .build();
  }

  public SdkResponseEventProto fromSdkResponseEventToProto(SdkResponseEvent sdkResponseEvent) {
    return SdkResponseEventProto.newBuilder()
        .setSdkResponseEventRequest(sdkResponseEvent.getSdkResponseEventRequest())
        .setSdkResponseEventType(sdkResponseEvent.getSdkResponseEventType())
        .build();
  }
}
