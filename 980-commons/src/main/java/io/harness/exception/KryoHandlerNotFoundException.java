package io.harness.exception;

import static io.harness.eraro.ErrorCode.KRYO_HANDLER_NOT_FOUND_ERROR;

import io.harness.eraro.Level;

public class KryoHandlerNotFoundException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public KryoHandlerNotFoundException(String message) {
    super(message, null, KRYO_HANDLER_NOT_FOUND_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }

  public KryoHandlerNotFoundException(String message, Throwable cause) {
    super(message, cause, KRYO_HANDLER_NOT_FOUND_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }
}