/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Value
@Builder
@Slf4j
public class ExecutionEngineDispatcher implements Runnable {
  Ambiance ambiance;
  OrchestrationEngine orchestrationEngine;

  @Override
  public void run() {
    orchestrationEngine.startNodeExecution(AmbianceUtils.obtainCurrentRuntimeId(ambiance));
  }
}
