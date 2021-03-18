package io.harness.exception;

import static io.harness.eraro.ErrorCode.DELEGATE_ERROR_HANDLER_ERROR;

import io.harness.eraro.Level;

public class DelegateErrorHandlerException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public DelegateErrorHandlerException(String message) {
    super(message, null, DELEGATE_ERROR_HANDLER_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }

  public DelegateErrorHandlerException(String message, Throwable cause) {
    super(message, cause, DELEGATE_ERROR_HANDLER_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
