package io.harness.skip.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EphemeralOrchestrationGraph;

@OwnedBy(CDC)
public interface VertexSkipperService {
  void removeSkippedVertices(EphemeralOrchestrationGraph orchestrationGraph);
}
