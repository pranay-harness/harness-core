/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception.runtime;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Data;

@Data
@OwnedBy(HarnessTeam.PIPELINE)
public class GcpClientRuntimeException extends RuntimeException {
  private String message;
  Throwable cause;

  public GcpClientRuntimeException(String message) {
    this.message = message;
  }

  public GcpClientRuntimeException(String message, Throwable cause) {
    this.message = message;
    this.cause = cause;
  }
}
