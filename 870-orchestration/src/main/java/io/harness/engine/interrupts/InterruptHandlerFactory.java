package io.harness.engine.interrupts;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.interrupts.handlers.AbortAllInterruptHandler;
import io.harness.engine.interrupts.handlers.AbortInterruptHandler;
import io.harness.engine.interrupts.handlers.CustomFailureInterruptHandler;
import io.harness.engine.interrupts.handlers.IgnoreFailedInterruptHandler;
import io.harness.engine.interrupts.handlers.MarkExpiredInterruptHandler;
import io.harness.engine.interrupts.handlers.MarkFailedInterruptHandler;
import io.harness.engine.interrupts.handlers.MarkSuccessInterruptHandler;
import io.harness.engine.interrupts.handlers.PauseAllInterruptHandler;
import io.harness.engine.interrupts.handlers.ResumeAllInterruptHandler;
import io.harness.engine.interrupts.handlers.RetryInterruptHandler;
import io.harness.pms.contracts.interrupts.InterruptType;

import com.google.inject.Inject;

@OwnedBy(PIPELINE)
public class InterruptHandlerFactory {
  @Inject private AbortAllInterruptHandler abortAllInterruptHandler;
  @Inject private PauseAllInterruptHandler pauseAllInterruptHandler;
  @Inject private ResumeAllInterruptHandler resumeAllInterruptHandler;
  @Inject private RetryInterruptHandler retryInterruptHandler;
  @Inject private MarkExpiredInterruptHandler markExpiredInterruptHandler;
  @Inject private MarkSuccessInterruptHandler markSuccessInterruptHandler;
  @Inject private MarkFailedInterruptHandler markFailedInterruptHandler;
  @Inject private IgnoreFailedInterruptHandler ignoreFailedInterruptHandler;
  @Inject private CustomFailureInterruptHandler customFailureInterruptHandler;
  @Inject private AbortInterruptHandler abortInterruptHandler;

  public InterruptHandler obtainHandler(InterruptType interruptType) {
    switch (interruptType) {
      case ABORT_ALL:
        return abortAllInterruptHandler;
      case PAUSE_ALL:
        return pauseAllInterruptHandler;
      case RESUME_ALL:
        return resumeAllInterruptHandler;
      case RETRY:
        return retryInterruptHandler;
      case MARK_EXPIRED:
        return markExpiredInterruptHandler;
      case MARK_SUCCESS:
        return markSuccessInterruptHandler;
      case IGNORE:
        return ignoreFailedInterruptHandler;
      case MARK_FAILED:
        return markFailedInterruptHandler;
      case CUSTOM_FAILURE:
        return customFailureInterruptHandler;
      case ABORT:
        return abortInterruptHandler;
      default:
        throw new IllegalStateException("No Handler Available for Interrupt Type: " + interruptType);
    }
  }
}
