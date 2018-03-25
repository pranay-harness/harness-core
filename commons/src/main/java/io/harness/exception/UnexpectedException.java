package io.harness.exception;

public class UnexpectedException extends RuntimeException {
  public UnexpectedException() {}
  public UnexpectedException(String message) {
    super(message);
  }
}
