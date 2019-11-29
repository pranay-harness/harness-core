package io.harness.exception;

import static java.lang.String.format;

import io.harness.eraro.Level;

public class UnknownEnumTypeException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public UnknownEnumTypeException(String typeDisplayName, String value) {
    super(null, null, null, Level.ERROR, null, null);
    super.param(MESSAGE_KEY, format("Unknown %s: %s", typeDisplayName, value));
  }
}
