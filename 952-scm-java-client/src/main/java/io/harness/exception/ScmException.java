/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

@OwnedBy(DX)
public class ScmException extends WingsException {
  public ScmException(ErrorCode errorCode) {
    super("", null, errorCode, Level.ERROR, USER, null);
  }
  public ScmException(String message, Throwable cause, ErrorCode errorCode) {
    super(message, cause, errorCode, Level.ERROR, USER, null);
  }
}
