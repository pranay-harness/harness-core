package io.harness.pms.sdk.core.response.publishers;

import io.harness.pms.execution.SdkResponseEvent;

public interface SdkResponseEventPublisher {
  void publishEvent(SdkResponseEvent event);
}
