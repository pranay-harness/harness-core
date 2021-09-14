/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.exception;

import static io.harness.eraro.ErrorCode.BASELINE_CONFIGURATION_ERROR;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class InvalidBaselineConfigurationException extends WingsException {
  public InvalidBaselineConfigurationException(String message) {
    super(message, null, BASELINE_CONFIGURATION_ERROR, Level.ERROR, null, null);
  }
}
