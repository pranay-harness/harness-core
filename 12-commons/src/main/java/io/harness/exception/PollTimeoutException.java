package io.harness.exception;

import static io.harness.eraro.ErrorCode.UNKNOWN_ERROR;

import io.harness.eraro.Level;

import java.time.Duration;

public class PollTimeoutException extends WingsException {
  public PollTimeoutException(Duration timeout) {
    super("The condition was not met after " + timeout.toString(), null, UNKNOWN_ERROR, Level.ERROR, null, null);
  }
}
