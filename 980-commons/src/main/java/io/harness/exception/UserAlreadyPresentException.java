/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.USER_ALREADY_PRESENT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

@OwnedBy(PL)
@SuppressWarnings("squid:CallToDeprecatedMethod")
public class UserAlreadyPresentException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public UserAlreadyPresentException(String message) {
    super(message, null, USER_ALREADY_PRESENT, Level.INFO, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
