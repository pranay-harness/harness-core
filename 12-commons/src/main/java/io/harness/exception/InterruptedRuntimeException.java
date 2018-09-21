package io.harness.exception;

public class InterruptedRuntimeException extends RuntimeException {
  public InterruptedRuntimeException(InterruptedException exception) {
    super(exception);
  }
}
