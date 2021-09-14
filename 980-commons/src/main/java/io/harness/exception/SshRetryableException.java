/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.SSH_CONNECTION_ERROR;

import io.harness.eraro.Level;

public class SshRetryableException extends WingsException {
  public SshRetryableException(Throwable cause) {
    super("Ssh retryable error", cause, SSH_CONNECTION_ERROR, Level.ERROR, NOBODY, null);
  }
}
