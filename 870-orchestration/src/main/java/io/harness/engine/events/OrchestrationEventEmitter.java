package io.harness.engine.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.StepTypeLookupService;
import io.harness.logging.AutoLogContext;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationSubject;
import io.harness.pms.sdk.core.registries.OrchestrationEventHandlerRegistry;
import io.harness.pms.utils.PmsConstants;
import io.harness.queue.QueuePublisher;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDC)
@Slf4j
public class OrchestrationEventEmitter {
  @Inject private OrchestrationEventHandlerRegistry handlerRegistry;
  @Inject private QueuePublisher<OrchestrationEvent> orchestrationEventQueue;
  @Inject(optional = true) private StepTypeLookupService stepTypeLookupService;

  public void emitEvent(OrchestrationEvent event) {
    try (AutoLogContext ignore = event.autoLogContext()) {
      OrchestrationSubject subject = new OrchestrationSubject();
      Set<OrchestrationEventHandler> handlers = handlerRegistry.obtain(event.getEventType());
      subject.registerAll(handlers);
      subject.handleEventSync(event);
      if (stepTypeLookupService == null || event.getNodeExecutionProto() == null) {
        orchestrationEventQueue.send(event);
      } else {
        String serviceName = event.getNodeExecutionProto().getNode().getServiceName();
        orchestrationEventQueue.send(Collections.singletonList(serviceName), event);
        // For calling event handlers in PMS, create a one level clone of the event and then emit
        if (!serviceName.equals(PmsConstants.INTERNAL_SERVICE_NAME)) {
          orchestrationEventQueue.send(Collections.singletonList(PmsConstants.INTERNAL_SERVICE_NAME),
              OrchestrationEvent.builder()
                  .ambiance(event.getAmbiance())
                  .nodeExecutionProto(event.getNodeExecutionProto())
                  .eventType(event.getEventType())
                  .build());
        }
      }
    } catch (Exception ex) {
      log.error("Failed to create orchestration event", ex);
    }
  }
}
