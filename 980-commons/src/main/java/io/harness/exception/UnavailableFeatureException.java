/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.FEATURE_UNAVAILABLE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

@OwnedBy(HarnessTeam.PL)
public class UnavailableFeatureException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public UnavailableFeatureException(String message) {
    super(null, null, FEATURE_UNAVAILABLE, Level.ERROR, null, null);
    super.param(MESSAGE_KEY, message);
  }
}
