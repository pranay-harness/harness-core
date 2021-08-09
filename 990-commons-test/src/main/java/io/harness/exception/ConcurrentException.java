package io.harness.exception;

public class ConcurrentException extends RuntimeException {
  public ConcurrentException(Exception cause) {
    super(cause);
  }
}
