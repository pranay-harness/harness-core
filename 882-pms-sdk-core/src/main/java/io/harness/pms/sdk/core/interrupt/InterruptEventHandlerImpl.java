package io.harness.pms.sdk.core.interrupt;

import static io.harness.govern.Switch.noop;

import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.interrupts.InterruptEvent;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.execution.utils.InterruptEventUtils;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InterruptEventHandlerImpl implements InterruptEventHandler {
  @Inject private InterruptEventListenerHelper interruptEventListenerHelper;

  @Override
  public boolean handleEvent(InterruptEvent event) {
    boolean handled = false;
    try (AutoLogContext ignore = InterruptEventUtils.obtainLogContext(event)) {
      InterruptType interruptType = event.getType();
      switch (interruptType) {
        case ABORT:
          interruptEventListenerHelper.handleAbort(event);
          log.info("[PMS_SDK] Handled ABORT InterruptEvent Successfully");
          handled = true;
          break;
        case CUSTOM_FAILURE:
          interruptEventListenerHelper.handleFailure(event);
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
