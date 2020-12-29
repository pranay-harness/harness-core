package io.harness.exceptions;

public class CastedFieldException extends RuntimeException {
  public CastedFieldException(final String message) {
    super(message);
  }

  public CastedFieldException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
