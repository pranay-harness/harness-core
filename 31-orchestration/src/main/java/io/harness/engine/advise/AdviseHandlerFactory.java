package io.harness.engine.advise;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.adviser.AdviseType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.advise.handlers.NextStepHandler;
import io.harness.engine.advise.handlers.RetryAdviseHandler;
import io.harness.exception.InvalidRequestException;

@OwnedBy(CDC)
public class AdviseHandlerFactory {
  @Inject private NextStepHandler nextStepHandler;
  @Inject private RetryAdviseHandler retryAdviseHandler;

  public AdviseHandler obtainHandler(AdviseType adviseType) {
    switch (adviseType) {
      case NEXT_STEP:
        return nextStepHandler;
      case RETRY:
        return retryAdviseHandler;
      default:
        throw new InvalidRequestException("No handler Present for advise type: " + adviseType);
    }
  }
}
