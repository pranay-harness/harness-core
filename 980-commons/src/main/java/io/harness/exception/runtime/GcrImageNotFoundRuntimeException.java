package io.harness.exception.runtime;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public class GcrImageNotFoundRuntimeException extends GcpClientRuntimeException {
  public GcrImageNotFoundRuntimeException(String message) {
    super(message);
  }

  public GcrImageNotFoundRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }
}
