package io.harness.engine.interrupts;

import com.google.inject.Inject;

import io.harness.engine.interrupts.handlers.AbortAllHandler;
import io.harness.interrupts.ExecutionInterruptType;

public class InterruptHandlerFactory {
  @Inject private AbortAllHandler abortAllHandler;

  public InterruptHandler obtainHandler(ExecutionInterruptType interruptType) {
    switch (interruptType) {
      case ABORT_ALL:
        return abortAllHandler;
      default:
        throw new IllegalStateException("No Handler Available for Interrupt Type: " + interruptType);
    }
  }
}
