package io.harness.distribution.idempotence;

public class UnableToRegisterIdempotentOperationException extends Exception {
  public UnableToRegisterIdempotentOperationException(String message) {
    super(message);
  }

  public UnableToRegisterIdempotentOperationException(Exception cause) {
    super(cause);
  }
}
