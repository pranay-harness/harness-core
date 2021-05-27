package io.harness.pms.sdk.core.interrupt;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.govern.Switch.noop;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.interrupts.InterruptEvent;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
public class InterruptEventListener extends QueueListener<InterruptEvent> {
  @Inject InterruptEventListenerHelper interruptEventListenerHelper;

  @Inject
  public InterruptEventListener(QueueConsumer<InterruptEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  @Override
  public void onMessage(InterruptEvent event) {
    try (AutoLogContext ignore = event.autoLogContext()) {
      InterruptType interruptType = event.getInterruptType();
      switch (interruptType) {
        case ABORT:
          interruptEventListenerHelper.handleAbort(event.getNodeExecution(), event.getNotifyId());
          break;
        case CUSTOM_FAILURE:
          interruptEventListenerHelper.handleFailure(
              event.getNodeExecution(), event.getMetadata(), event.getInterruptUuid(), event.getNotifyId());
          break;
        default:
          log.warn("No Handling present for Interrupt Event of type : {}", interruptType);
          noop();
      }
    }
  }
}
