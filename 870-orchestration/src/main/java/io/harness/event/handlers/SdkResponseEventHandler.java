package io.harness.event.handlers;

import io.harness.pms.execution.SdkResponseEvent;

public interface SdkResponseEventHandler {
  void handleEvent(SdkResponseEvent event);
}
