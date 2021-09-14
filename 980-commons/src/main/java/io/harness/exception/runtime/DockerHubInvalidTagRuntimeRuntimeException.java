/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
