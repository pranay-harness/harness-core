/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.shell;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.logging.CommandExecutionStatus;
import io.harness.shell.ExecuteCommandResponse;

import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@Builder
public class ShellScriptTaskResponseNG implements DelegateTaskNotifyResponseData {
  @NonFinal @Setter DelegateMetaInfo delegateMetaInfo;
  ExecuteCommandResponse executeCommandResponse;
  CommandExecutionStatus status;
  String errorMessage;
  UnitProgressData unitProgressData;
}
