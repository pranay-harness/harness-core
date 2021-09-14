/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.steps.executables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.sdk.core.steps.io.PassThroughData;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder(buildMethodName = "internalBuild")
public class TaskChainResponse {
  boolean chainEnd;
  PassThroughData passThroughData;
  TaskRequest taskRequest;
  List<String> logKeys;
  List<String> units;

  public static class TaskChainResponseBuilder {
    public TaskChainResponse build() {
      if (taskRequest == null && !chainEnd) {
        throw new InvalidRequestException("Task Cannot be null if not chain end");
      }
      return internalBuild();
    }
  }
}
