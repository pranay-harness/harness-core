package io.harness.exceptions;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public class DuplicateAliasException extends RuntimeException {
  public DuplicateAliasException(final String message) {
    super(message);
  }
  public DuplicateAliasException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
