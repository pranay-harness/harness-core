package io.harness.redesign.services;

import io.harness.annotations.Redesign;
import io.harness.state.execution.ExecutionInstance;

@Redesign
public interface CustomExecutionService {
  ExecutionInstance executeHttpSwitch();

  ExecutionInstance executeHttpFork();
}
