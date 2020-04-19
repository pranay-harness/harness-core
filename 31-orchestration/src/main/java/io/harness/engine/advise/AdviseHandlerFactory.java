package io.harness.engine.advise;

import com.google.inject.Inject;

import io.harness.engine.advise.handlers.OnSuccessHandler;
import io.harness.exception.InvalidRequestException;

public class AdviseHandlerFactory {
  @Inject private OnSuccessHandler onSuccessHandler;

  public AdviseHandler obtainHandler(String adviseType) {
    switch (adviseType) {
      case "ON_SUCCESS":
        return onSuccessHandler;
      default:
        throw new InvalidRequestException("No handler Present for advise type: " + adviseType);
    }
  }
}
