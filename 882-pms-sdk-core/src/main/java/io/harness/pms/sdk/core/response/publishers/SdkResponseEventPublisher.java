package io.harness.pms.sdk.core.response.publishers;

import io.harness.pms.contracts.execution.events.SdkResponseEventProto;

public interface SdkResponseEventPublisher {
  void publishEvent(SdkResponseEventProto event);
}
