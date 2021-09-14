/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.exception;

import static io.harness.eraro.ErrorCode.PMS_INITIALIZE_SDK_EXCEPTION;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class InitializeSdkException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public InitializeSdkException(String message) {
    super(message, null, PMS_INITIALIZE_SDK_EXCEPTION, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
