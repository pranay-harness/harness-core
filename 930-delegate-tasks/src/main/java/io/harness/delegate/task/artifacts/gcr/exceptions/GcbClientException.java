package io.harness.delegate.task.artifacts.gcr.exceptions;

public class GcbClientException extends RuntimeException {
  public GcbClientException(String message, Throwable cause) {
    super(message, cause);
  }
  public GcbClientException(String message) {
    super(message);
  }
}
