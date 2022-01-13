/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.citasks.cik8handler;

public enum ExecCommandStatus {
  SUCCESS, // Command execution completed with 0 exit code.
  FAILURE, // Command execution completed with non-zero exit code.
  ERROR, // Failed to execute command due to some error.
  TIMEOUT // Timeout in command execution
}
