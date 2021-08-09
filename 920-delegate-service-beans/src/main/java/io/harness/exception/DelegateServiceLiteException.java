package io.harness.exception;

public class DelegateServiceLiteException extends RuntimeException {
  public DelegateServiceLiteException(String message, Throwable cause) {
    super(message, cause);
  }

  public DelegateServiceLiteException(String message) {
    super(message);
  }
}
