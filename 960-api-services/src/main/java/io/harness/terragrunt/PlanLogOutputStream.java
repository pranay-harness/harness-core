/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.terragrunt;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.LogLevel.INFO;

import static com.google.common.base.Joiner.on;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import org.zeroturnaround.exec.stream.LogOutputStream;

@OwnedBy(CDP)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PlanLogOutputStream extends LogOutputStream {
  @Setter LogCallback logCallback;
  private List<String> logs;

  @Override
  public void processLine(String line) {
    logCallback.saveExecutionLog(line, INFO, CommandExecutionStatus.RUNNING);
    if (logs == null) {
      logs = new ArrayList<>();
    }
    logs.add(line);
  }

  public String getPlanLog() {
    if (isNotEmpty(logs)) {
      return on("\n").join(logs);
    }
    return "";
  }
}
