package io.harness.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.event.handlers.SdkResponseEventHandler;
import io.harness.logging.AutoLogContext;
import io.harness.pms.execution.SdkResponseEvent;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListenerWithObservers;
import io.harness.registries.SdkNodeExecutionEventHandlerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class SdkResponseEventListener extends QueueListenerWithObservers<SdkResponseEvent> {
  @Inject private SdkNodeExecutionEventHandlerFactory handlerRegistry;

  @Inject
  public SdkResponseEventListener(QueueConsumer<SdkResponseEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  @Override
  public void onMessageInternal(SdkResponseEvent event) {
    try (AutoLogContext ignore = event.autoLogContext()) {
      log.info("Event for SdkResponseEvent received");
      SdkResponseEventHandler handler = handlerRegistry.getHandler(event.getSdkResponseEventType());
      handler.handleEvent(event);
    } catch (Exception ex) {
      log.error("Exception Occurred while handling SdkResponseEvent", ex);
    }
  }
}
