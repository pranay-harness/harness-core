/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.USER_GROUP_ALREADY_EXIST;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

@OwnedBy(PL)
public class InvalidAccessRequestException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public InvalidAccessRequestException(String message) {
    super(message, null, USER_GROUP_ALREADY_EXIST, Level.INFO, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
