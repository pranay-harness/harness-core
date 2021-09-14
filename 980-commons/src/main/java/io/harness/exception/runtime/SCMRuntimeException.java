/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception.runtime;

import io.harness.eraro.ErrorCode;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class SCMRuntimeException extends RuntimeException {
  private final String message;
  private Throwable cause;
  private ErrorCode errorCode;

  public SCMRuntimeException(String message) {
    this.message = message;
  }

  public SCMRuntimeException(String message, ErrorCode errorCode) {
    this.message = message;
    this.errorCode = errorCode;
  }

  public SCMRuntimeException(String message, Throwable cause) {
    this.message = message;
    this.cause = cause;
  }

  public SCMRuntimeException(String message, Throwable cause, ErrorCode errorCode) {
    this.message = message;
    this.cause = cause;
    this.errorCode = errorCode;
  }
}
