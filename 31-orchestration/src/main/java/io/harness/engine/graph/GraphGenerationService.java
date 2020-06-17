package io.harness.engine.graph;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.presentation.Graph;

@OwnedBy(CDC)
@Redesign
public interface GraphGenerationService {
  Graph generateGraph(String planExecutionId);
}
