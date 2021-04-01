package io.harness.execution;

import io.harness.event.handlers.SdkResponseEventHandler;
import io.harness.logging.AutoLogContext;
import io.harness.pms.execution.SdkResponseEvent;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;
import io.harness.registries.SdkNodeExecutionEventHandlerFactory;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SdkResponseEventListener extends QueueListener<SdkResponseEvent> {
  @Inject private SdkNodeExecutionEventHandlerFactory handlerRegistry;

  @Inject
  public SdkResponseEventListener(QueueConsumer<SdkResponseEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  @Override
  public void onMessage(SdkResponseEvent event) {
    try (AutoLogContext ignore = event.autoLogContext()) {
      log.info("Event for SdkResponseEventType received");
      SdkResponseEventHandler handler = handlerRegistry.getHandler(event.getSdkResponseEventType());
      handler.handleEvent(event);
    } catch (Exception ex) {
      log.error("Exception Occurred while handling SdkResponseEvent", ex);
    }
  }
}
