package io.harness.exception;

import static io.harness.eraro.ErrorCode.ENTITY_REFERENCE_EXCEPTION;

import io.harness.eraro.Level;

public class ReferencedEntityException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public ReferencedEntityException(String message) {
    super(message, null, ENTITY_REFERENCE_EXCEPTION, Level.ERROR, null, null);
    param(MESSAGE_KEY, message);
  }
}
