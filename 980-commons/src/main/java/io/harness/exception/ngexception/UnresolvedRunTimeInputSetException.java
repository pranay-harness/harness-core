/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception.ngexception;

import static io.harness.eraro.ErrorCode.NG_PIPELINE_EXECUTION_EXCEPTION;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

import java.util.EnumSet;

public class UnresolvedRunTimeInputSetException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public UnresolvedRunTimeInputSetException(String message) {
    super(message, null, NG_PIPELINE_EXECUTION_EXCEPTION, Level.ERROR, null, null);
    param(MESSAGE_KEY, message);
  }

  public UnresolvedRunTimeInputSetException(String message, Throwable cause) {
    super(message, cause, NG_PIPELINE_EXECUTION_EXCEPTION, Level.ERROR, (EnumSet) null, (EnumSet) null);
  }
}
