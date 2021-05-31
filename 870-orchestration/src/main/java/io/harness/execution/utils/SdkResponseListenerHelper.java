package io.harness.execution.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.event.handlers.SdkResponseEventHandler;
import io.harness.logging.AutoLogContext;
import io.harness.pms.execution.SdkResponseEvent;
import io.harness.pms.execution.utils.SdkResponseEventUtils;
import io.harness.registries.SdkNodeExecutionEventHandlerFactory;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class SdkResponseListenerHelper {
  @Inject private SdkNodeExecutionEventHandlerFactory handlerRegistry;

  public void handleEvent(SdkResponseEvent sdkResponseEvent) {
    try (AutoLogContext ignore = SdkResponseEventUtils.obtainLogContext(sdkResponseEvent)) {
      log.info("Event for SdkResponseEvent received");
      SdkResponseEventHandler handler = handlerRegistry.getHandler(sdkResponseEvent.getSdkResponseEventType());
      handler.handleEvent(sdkResponseEvent);
    }
  }
}
