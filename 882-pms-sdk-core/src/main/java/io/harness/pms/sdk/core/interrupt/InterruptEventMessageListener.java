package io.harness.pms.sdk.core.interrupt;

import static io.harness.govern.Switch.noop;

import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.interrupts.InterruptEvent;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.execution.utils.InterruptEventUtils;
import io.harness.pms.sdk.core.execution.events.base.SdkBaseEventMessageListener;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InterruptEventMessageListener extends SdkBaseEventMessageListener<InterruptEvent> {
  @Inject private InterruptEventListenerHelper interruptEventListenerHelper;

  public InterruptEventMessageListener() {
    super(InterruptEvent.class);
  }

  public boolean processMessage(InterruptEvent event) {
    // We should always return true else the event will be redelivered
    boolean handled = false;
    try (AutoLogContext ignore = InterruptEventUtils.obtainLogContext(event)) {
      InterruptType interruptType = event.getType();
      switch (interruptType) {
        case ABORT:
          interruptEventListenerHelper.handleAbort(event.getNodeExecution(), event.getNotifyId());
          log.info("[PMS_SDK] Handled ABORT InterruptEvent Successfully");
          handled = true;
          break;
        case CUSTOM_FAILURE:
          interruptEventListenerHelper.handleFailure(
              event.getNodeExecution(), event.getMetadata(), event.getInterruptUuid(), event.getNotifyId());
          log.info("[PMS_SDK] Handled CUSTOM_FAILURE InterruptEvent Successfully");
          handled = true;
          break;
        default:
          log.warn("No Handling present for Interrupt Event of type : {}", interruptType);
          noop();
      }
    }
    return handled;
  }
}
