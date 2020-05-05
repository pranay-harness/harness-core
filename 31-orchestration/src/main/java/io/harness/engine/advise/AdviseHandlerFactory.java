package io.harness.engine.advise;

import com.google.inject.Inject;

import io.harness.adviser.AdviseType;
import io.harness.engine.advise.handlers.NextStepHandler;
import io.harness.exception.InvalidRequestException;

public class AdviseHandlerFactory {
  @Inject private NextStepHandler nextStepHandler;

  public AdviseHandler obtainHandler(AdviseType adviseType) {
    switch (adviseType) {
      case NEXT_STEP:
        return nextStepHandler;
      default:
        throw new InvalidRequestException("No handler Present for advise type: " + adviseType);
    }
  }
}
