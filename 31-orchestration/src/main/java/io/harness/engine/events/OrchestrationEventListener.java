package io.harness.engine.events;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.OrchestrationSubject;
import io.harness.logging.AutoLogContext;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;
import io.harness.registries.events.OrchestrationEventHandlerRegistry;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class OrchestrationEventListener extends QueueListener<OrchestrationEvent> {
  @Inject private OrchestrationEventHandlerRegistry handlerRegistry;

  @Inject
  public OrchestrationEventListener(QueueConsumer<OrchestrationEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  @Override
  public void onMessage(OrchestrationEvent event) {
    try (AutoLogContext ignore = event.autoLogContext()) {
      logger.info("Notifying for OrchestrationEvent");

      try {
        OrchestrationSubject subject = handlerRegistry.obtain(event.getEventType());
        subject.handleEventAsync(event);
      } catch (Exception ex) {
        logger.error("Exception Occurred while handling OrchestrationEvent", ex);
      }
    }
  }
}
