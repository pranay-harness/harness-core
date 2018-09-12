package software.wings.exception;

import io.harness.exception.WingsException;

public class UnexpectedException extends WingsException {
  public UnexpectedException() {
    super("This should not be happening");
  }
}
