/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logging;

import lombok.extern.log4j.Log4j;

@Log4j
public class DummyLogCallbackImpl implements LogCallback {
  @Override
  public void saveExecutionLog(String line) {
    log.info(line);
  }

  @Override
  public void saveExecutionLog(String line, LogLevel logLevel) {
    log.info(line);
  }

  @Override
  public void saveExecutionLog(String line, LogLevel logLevel, CommandExecutionStatus commandExecutionStatus) {
    log.info(line);
  }
}
