/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.terragrunt;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.logging.LogLevel.INFO;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import org.zeroturnaround.exec.stream.LogOutputStream;

@OwnedBy(CDP)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ActivityLogOutputStream extends LogOutputStream {
  @Setter LogCallback logCallback;

  @Override
  public void processLine(String line) {
    logCallback.saveExecutionLog(line, INFO, CommandExecutionStatus.RUNNING);
  }
}
