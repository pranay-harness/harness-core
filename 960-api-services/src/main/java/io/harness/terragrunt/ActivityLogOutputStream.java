/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
