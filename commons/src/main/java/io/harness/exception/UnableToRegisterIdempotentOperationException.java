package io.harness.exception;

public class UnableToRegisterIdempotentOperationException extends Exception {
  UnableToRegisterIdempotentOperationException(String message) {
    super(message);
  }
}
