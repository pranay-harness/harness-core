/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.shell;

import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExecuteCommandResponse {
  CommandExecutionStatus status;
  CommandExecutionData commandExecutionData;
}
