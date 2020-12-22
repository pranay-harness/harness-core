package io.harness.exception;

public class DelegateServiceDriverException extends RuntimeException {
  public DelegateServiceDriverException(String message, Throwable cause) {
    super(message, cause);
  }

  public DelegateServiceDriverException(String message) {
    super(message);
  }
}
