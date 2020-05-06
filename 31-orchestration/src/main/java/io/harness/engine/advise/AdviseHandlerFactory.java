package io.harness.engine.advise;

import com.google.inject.Inject;

import io.harness.adviser.AdviseType;
import io.harness.engine.advise.handlers.NextStepHandler;
import io.harness.engine.advise.handlers.RetryHandler;
import io.harness.exception.InvalidRequestException;

public class AdviseHandlerFactory {
  @Inject private NextStepHandler nextStepHandler;
  @Inject private RetryHandler retryHandler;

  public AdviseHandler obtainHandler(AdviseType adviseType) {
    switch (adviseType) {
      case NEXT_STEP:
        return nextStepHandler;
      case RETRY:
        return retryHandler;
      default:
        throw new InvalidRequestException("No handler Present for advise type: " + adviseType);
    }
  }
}
