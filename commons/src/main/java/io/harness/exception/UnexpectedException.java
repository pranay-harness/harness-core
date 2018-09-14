package io.harness.exception;

import static io.harness.eraro.ErrorCode.UNEXPECTED;

public class UnexpectedException extends WingsException {
  public UnexpectedException() {
    super(UNEXPECTED, "This should not be happening");
  }
  public UnexpectedException(String message) {
    super(UNEXPECTED, message);
  }
}
