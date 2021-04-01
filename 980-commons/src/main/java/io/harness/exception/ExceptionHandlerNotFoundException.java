package io.harness.exception;

import static io.harness.eraro.ErrorCode.EXCEPTION_HANDLER_NOT_FOUND;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

@OwnedBy(HarnessTeam.DX)
public class ExceptionHandlerNotFoundException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public ExceptionHandlerNotFoundException(String message) {
    super(message, null, EXCEPTION_HANDLER_NOT_FOUND, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
