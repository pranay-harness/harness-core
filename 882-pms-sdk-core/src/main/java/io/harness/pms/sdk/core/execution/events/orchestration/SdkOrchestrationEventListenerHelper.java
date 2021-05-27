package io.harness.pms.sdk.core.execution.events.orchestration;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationSubject;
import io.harness.pms.sdk.core.registries.OrchestrationEventHandlerRegistry;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Slf4j
@Singleton
public class SdkOrchestrationEventListenerHelper {
  @Inject private OrchestrationEventHandlerRegistry handlerRegistry;

  public void handleEvent(OrchestrationEvent orchestrationEvent) {
    Set<OrchestrationEventHandler> handlers = handlerRegistry.obtain(orchestrationEvent.getEventType());
    OrchestrationSubject subject = new OrchestrationSubject();
    subject.registerAll(handlers);
    subject.handleEventAsync(orchestrationEvent);
  }
}
