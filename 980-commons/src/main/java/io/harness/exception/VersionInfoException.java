package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(DEL)
public class VersionInfoException extends RuntimeException {
  public VersionInfoException(Throwable exception) {
    super(exception);
  }
}
