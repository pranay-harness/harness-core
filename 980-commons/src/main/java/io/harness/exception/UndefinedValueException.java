package io.harness.exception;

import static io.harness.eraro.ErrorCode.ILLEGAL_STATE;

import io.harness.eraro.Level;

public class UndefinedValueException extends WingsException {
  public UndefinedValueException(String message) {
    super(message, null, ILLEGAL_STATE, Level.ERROR, null, null);
    param("message", message);
  }
}
