package io.harness.service;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Graph;

@OwnedBy(HarnessTeam.CDC)
@Redesign
public interface GraphGenerationService {
  Graph generateGraph(String planExecutionId);
}
