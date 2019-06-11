package io.harness.exception;

import java.time.Duration;

public class PollTimeoutException extends WingsException {
  public PollTimeoutException(Duration timeout) {
    super("The condition was not met after " + timeout.toString());
  }
}
