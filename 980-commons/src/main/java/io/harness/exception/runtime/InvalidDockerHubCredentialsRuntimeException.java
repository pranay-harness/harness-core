/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception.runtime;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;

@OwnedBy(HarnessTeam.PIPELINE)
public class InvalidDockerHubCredentialsRuntimeException extends DockerHubServerRuntimeException {
  public InvalidDockerHubCredentialsRuntimeException(String message) {
    super(message);
  }

  public InvalidDockerHubCredentialsRuntimeException(String message, ErrorCode code) {
    super(message, code);
  }

  public InvalidDockerHubCredentialsRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidDockerHubCredentialsRuntimeException(String message, Throwable cause, ErrorCode code) {
    super(message, cause, code);
  }
}
