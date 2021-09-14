/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logging;

public class NoopExecutionCallback implements LogCallback {
  @Override
  public void saveExecutionLog(String line) {}

  @Override
  public void saveExecutionLog(String line, LogLevel logLevel) {}

  @Override
  public void saveExecutionLog(String line, LogLevel logLevel, CommandExecutionStatus commandExecutionStatus) {
    // Nothing required
  }
}
