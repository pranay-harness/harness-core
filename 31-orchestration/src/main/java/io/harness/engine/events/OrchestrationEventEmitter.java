package io.harness.engine.events;

import com.google.inject.Inject;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.OrchestrationEventHandlerAsyncWrapper;
import io.harness.execution.events.OrchestrationSubject;
import io.harness.logging.AutoLogContext;
import io.harness.registries.events.OrchestrationEventHandlerRegistry;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDC)
@Slf4j
public class OrchestrationEventEmitter {
  @Inject private OrchestrationEventHandlerRegistry handlerRegistry;

  public void emitEvent(OrchestrationEvent event) {
    try (AutoLogContext ignore = event.autoLogContext()) {
      OrchestrationSubject subject = handlerRegistry.obtain(event.getEventType());
      subject.fireInform(OrchestrationEventHandlerAsyncWrapper::fire, event);
    } catch (Exception ex) {
      logger.error("Exception Occurred while emitting event : {}", ex.getMessage());
    }
  }
}
