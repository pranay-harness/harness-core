/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.pms.advise;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.advisers.AdviserResponse;

@OwnedBy(CDC)
public interface AdviserResponseHandler {
  void handleAdvise(NodeExecution nodeExecution, AdviserResponse advise);
}
