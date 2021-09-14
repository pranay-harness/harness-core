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

import lombok.Data;

@Data
@OwnedBy(HarnessTeam.PIPELINE)
public class DockerHubServerRuntimeException extends RuntimeException {
  private String message;
  Throwable cause;
  ErrorCode code = ErrorCode.INVALID_CREDENTIAL;

  public DockerHubServerRuntimeException(String message) {
    this.message = message;
  }

  public DockerHubServerRuntimeException(String message, ErrorCode code) {
    this.message = message;
    this.code = code;
  }

  public DockerHubServerRuntimeException(String message, Throwable cause) {
    this.message = message;
    this.cause = cause;
  }

  public DockerHubServerRuntimeException(String message, Throwable cause, ErrorCode code) {
    this.message = message;
    this.cause = cause;
    this.code = code;
  }
}
