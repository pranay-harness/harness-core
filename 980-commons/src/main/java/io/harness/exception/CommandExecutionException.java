/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.COMMAND_EXECUTION_ERROR;

import io.harness.eraro.Level;

public class CommandExecutionException extends WingsException {
  private static final String REASON_ARG = "reason";

  public CommandExecutionException(String reason) {
    this(reason, null);
  }

  public CommandExecutionException(String reason, Throwable cause) {
    super(null, cause, COMMAND_EXECUTION_ERROR, Level.ERROR, null, null);
    super.param(REASON_ARG, reason);
  }
}
