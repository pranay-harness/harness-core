package io.harness.pms.sdk.response.publishers;

import io.harness.pms.execution.SdkResponseEvent;

public interface SdkResponseEventPublisher {
  void publishEvent(SdkResponseEvent event);
}
