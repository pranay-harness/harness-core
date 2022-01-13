/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cli;

import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import org.zeroturnaround.exec.stream.LogOutputStream;

public class LogCallbackOutputStream extends LogOutputStream {
  private LogCallback logCallback;

  public LogCallbackOutputStream(LogCallback logCallback) {
    this.logCallback = logCallback;
  }

  @Override
  protected void processLine(String line) {
    logCallback.saveExecutionLog(line, LogLevel.INFO, CommandExecutionStatus.RUNNING);
  }
}
