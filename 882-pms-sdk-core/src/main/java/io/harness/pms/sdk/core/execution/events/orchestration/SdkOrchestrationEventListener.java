package io.harness.pms.sdk.core.execution.events.orchestration;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListenerWithObservers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class SdkOrchestrationEventListener extends QueueListenerWithObservers<OrchestrationEvent> {
  @Inject private SdkOrchestrationEventListenerHelper helper;

  @Inject
  public SdkOrchestrationEventListener(QueueConsumer<OrchestrationEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  @Override
  public void onMessageInternal(OrchestrationEvent event) {
    try (AutoLogContext ignore = event.autoLogContext()) {
      log.info("Notifying for OrchestrationEvent");
      try {
        helper.handleEvent(event);
      } catch (Exception ex) {
        log.error("Exception Occurred while handling OrchestrationEvent", ex);
      }
    }
  }
}
