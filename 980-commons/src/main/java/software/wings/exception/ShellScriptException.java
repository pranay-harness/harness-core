/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

import java.util.EnumSet;

public class ShellScriptException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public ShellScriptException(String message, ErrorCode code, Level level, EnumSet<ReportTarget> reportTargets) {
    super(message, null, code, level, reportTargets, null);
    param(MESSAGE_KEY, message);
  }
}
