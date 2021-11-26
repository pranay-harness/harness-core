package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(DEL)
public class VersionInfoException extends RuntimeException {
  public VersionInfoException(String message, Throwable exception) {
    super(message, exception);
  }
  public VersionInfoException(Throwable exception) {
    super(exception);
  }
}
