package io.harness.engine.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.OrchestrationEventHandler;
import io.harness.execution.events.OrchestrationSubject;
import io.harness.logging.AutoLogContext;
import io.harness.queue.QueuePublisher;
import io.harness.registries.events.OrchestrationEventHandlerRegistry;

import com.google.inject.Inject;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDC)
@Slf4j
public class OrchestrationEventEmitter {
  @Inject private OrchestrationEventHandlerRegistry handlerRegistry;
  @Inject private QueuePublisher<OrchestrationEvent> orchestrationEventQueue;

  public void emitEvent(OrchestrationEvent event) {
    try (AutoLogContext ignore = event.autoLogContext()) {
      OrchestrationSubject subject = new OrchestrationSubject();
      Set<OrchestrationEventHandler> handlers = handlerRegistry.obtain(event.getEventType());
      subject.registerAll(handlers);
      subject.handleEventSync(event);
      orchestrationEventQueue.send(event);
    } catch (Exception ex) {
      log.error("Failed to create orchestration event", ex);
    }
  }
}
