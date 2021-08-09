package io.harness.exception;

public class LoadSourceCodeException extends RuntimeException {
  public LoadSourceCodeException(Exception e) {
    super(e);
  }
}
