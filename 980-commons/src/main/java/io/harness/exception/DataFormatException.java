/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.eraro.Level.ERROR;

import io.harness.eraro.ErrorCode;

/**
 * Indicates an exception due to data format issues.
 */
public class DataFormatException extends WingsException {
  // This is a new method, and does not override any deprecated method.
  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public DataFormatException(String message, Throwable cause) {
    super(message, cause, ErrorCode.UNKNOWN_ERROR, ERROR, SRE, null);
  }
}
