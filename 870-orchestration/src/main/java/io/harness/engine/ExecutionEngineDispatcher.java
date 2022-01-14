package io.harness.engine;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;

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
    orchestrationEngine.startNodeExecution(ambiance);
  }
}
