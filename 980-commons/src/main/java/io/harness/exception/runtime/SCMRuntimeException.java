/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
