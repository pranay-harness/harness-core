package io.harness.exception;

import java.time.Duration;

public class PullTimeoutException extends RuntimeException {
  public PullTimeoutException(Duration timeout) {
    super("The condition was not met after " + timeout.toString());
  }
}
