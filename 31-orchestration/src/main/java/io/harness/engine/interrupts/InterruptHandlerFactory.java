package io.harness.engine.interrupts;

import com.google.inject.Inject;

import io.harness.engine.interrupts.handlers.AbortAllHandler;
import io.harness.engine.interrupts.handlers.PauseAllHandler;
import io.harness.engine.interrupts.handlers.ResumeAllHandler;
import io.harness.interrupts.ExecutionInterruptType;

public class InterruptHandlerFactory {
  @Inject private AbortAllHandler abortAllHandler;
  @Inject private PauseAllHandler pauseAllHandler;
  @Inject private ResumeAllHandler resumeAllHandler;

  public InterruptHandler obtainHandler(ExecutionInterruptType interruptType) {
    switch (interruptType) {
      case ABORT_ALL:
        return abortAllHandler;
      case PAUSE_ALL:
        return pauseAllHandler;
      case RESUME_ALL:
        return resumeAllHandler;
      default:
        throw new IllegalStateException("No Handler Available for Interrupt Type: " + interruptType);
    }
  }
}
