package io.harness.engine;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Value
@Builder
@Slf4j
@Redesign
public class ExecutionEngineDispatcher implements Runnable {
  Ambiance ambiance;
  OrchestrationEngine orchestrationEngine;

  @Override
  public void run() {
    orchestrationEngine.startNodeExecution(ambiance);
  }
}
