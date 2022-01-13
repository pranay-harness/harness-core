/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.INVALID_REQUEST;

import static java.lang.String.format;

import io.harness.eraro.Level;

public class UnknownEnumTypeException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public UnknownEnumTypeException(String typeDisplayName, String value) {
    super(null, null, INVALID_REQUEST, Level.ERROR, null, null);
    super.param(MESSAGE_KEY, format("Unknown %s: %s", typeDisplayName, value));
  }
}
