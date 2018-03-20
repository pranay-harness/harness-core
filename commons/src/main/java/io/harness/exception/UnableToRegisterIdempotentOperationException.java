package io.harness.exception;

public class UnableToRegisterIdempotentOperationException extends Exception {
  public UnableToRegisterIdempotentOperationException(String message) {
    super(message);
  }

  public UnableToRegisterIdempotentOperationException(Exception cause) {
    super(cause);
  }
}
