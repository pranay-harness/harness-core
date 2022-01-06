/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.enforcement.exceptions;

import static io.harness.eraro.ErrorCode.INVALID_REQUEST;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class FeatureNotSupportedException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public FeatureNotSupportedException(String message) {
    super(message, null, INVALID_REQUEST, Level.ERROR, USER_SRE, null);
    super.param(MESSAGE_ARG, message);
  }
}
