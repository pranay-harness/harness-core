package io.harness.engine.interrupts;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.interrupts.handlers.AbortAllInterruptHandler;
import io.harness.engine.interrupts.handlers.PauseAllInterruptHandler;
import io.harness.engine.interrupts.handlers.ResumeAllInterruptHandler;
import io.harness.engine.interrupts.handlers.RetryInterruptHandler;
import io.harness.interrupts.ExecutionInterruptType;

@OwnedBy(CDC)
public class InterruptHandlerFactory {
  @Inject private AbortAllInterruptHandler abortAllInterruptHandler;
  @Inject private PauseAllInterruptHandler pauseAllInterruptHandler;
  @Inject private ResumeAllInterruptHandler resumeAllInterruptHandler;
  @Inject private RetryInterruptHandler retryInterruptHandler;

  public InterruptHandler obtainHandler(ExecutionInterruptType interruptType) {
    switch (interruptType) {
      case ABORT_ALL:
        return abortAllInterruptHandler;
      case PAUSE_ALL:
        return pauseAllInterruptHandler;
      case RESUME_ALL:
        return resumeAllInterruptHandler;
      case RETRY:
        return retryInterruptHandler;
      default:
        throw new IllegalStateException("No Handler Available for Interrupt Type: " + interruptType);
    }
  }
}
