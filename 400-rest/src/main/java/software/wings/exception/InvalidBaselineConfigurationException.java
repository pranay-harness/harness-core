/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
