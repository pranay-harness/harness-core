package io.harness.engine.advise;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.advisers.AdviserResponse;

@OwnedBy(CDC)
public interface AdviserResponseHandler {
  void handleAdvise(NodeExecution nodeExecution, AdviserResponse advise);
}
