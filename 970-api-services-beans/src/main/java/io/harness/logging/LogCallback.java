/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logging;

public interface LogCallback {
  void saveExecutionLog(String line);

  void saveExecutionLog(String line, LogLevel logLevel);

  void saveExecutionLog(String line, LogLevel logLevel, CommandExecutionStatus commandExecutionStatus);
}
