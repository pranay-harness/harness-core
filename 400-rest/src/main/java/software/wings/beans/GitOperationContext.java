/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.yaml.GitCommitRequest;
import software.wings.beans.yaml.GitDiffRequest;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@BreakDependencyOn("software.wings.beans.GitConfig")
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class GitOperationContext {
  private String gitConnectorId;
  private GitConfig gitConfig;

  private GitCommitRequest gitCommitRequest;
  private GitDiffRequest gitDiffRequest;
}
