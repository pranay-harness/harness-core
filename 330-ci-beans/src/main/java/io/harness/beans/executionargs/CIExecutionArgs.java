/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.executionargs;

import io.harness.beans.execution.ExecutionSource;
import io.harness.beans.inputset.InputSet;
import io.harness.ci.beans.entities.BuildNumberDetails;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@AllArgsConstructor
@Slf4j
public class CIExecutionArgs implements ExecutionArgs {
  private InputSet inputSet;
  private String branch;
  private ExecutionSource executionSource;
  private BuildNumberDetails buildNumberDetails;
}
