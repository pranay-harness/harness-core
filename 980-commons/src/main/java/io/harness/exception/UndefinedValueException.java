/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.ILLEGAL_STATE;

import io.harness.eraro.Level;

public class UndefinedValueException extends WingsException {
  public UndefinedValueException(String message) {
    super(message, null, ILLEGAL_STATE, Level.ERROR, null, null);
    param("message", message);
  }
}
