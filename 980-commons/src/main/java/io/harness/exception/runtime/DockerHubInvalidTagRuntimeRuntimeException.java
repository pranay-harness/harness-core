package io.harness.exception.runtime;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public class DockerHubInvalidTagRuntimeRuntimeException extends DockerHubServerRuntimeException {
  public DockerHubInvalidTagRuntimeRuntimeException(String message) {
    super(message);
  }

  public DockerHubInvalidTagRuntimeRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }
}
