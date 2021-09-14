/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.citasks.cik8handler;

public enum ExecCommandStatus {
  SUCCESS, // Command execution completed with 0 exit code.
  FAILURE, // Command execution completed with non-zero exit code.
  ERROR, // Failed to execute command due to some error.
  TIMEOUT // Timeout in command execution
}
