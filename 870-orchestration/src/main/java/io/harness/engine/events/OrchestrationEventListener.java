package io.harness.engine.events;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationSubject;
import io.harness.pms.sdk.registries.OrchestrationEventHandlerRegistry;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;

import com.google.inject.Inject;
import java.util.Set;
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
      log.info("Notifying for OrchestrationEvent");

      try {
        Set<OrchestrationEventHandler> handlers = handlerRegistry.obtain(event.getEventType());
        OrchestrationSubject subject = new OrchestrationSubject();
        subject.registerAll(handlers);
        subject.handleEventAsync(event);
      } catch (Exception ex) {
        log.error("Exception Occurred while handling OrchestrationEvent", ex);
      }
    }
  }
}
