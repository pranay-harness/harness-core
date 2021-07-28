package io.harness.graph.stepDetail.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.data.OrchestrationMap;

import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PmsGraphStepDetailsService {
  void addStepDetail(String nodeExecutionId, String planExecutionId, OrchestrationMap stepDetails, String name);
  Map<String, OrchestrationMap> getStepDetails(String planExecutionId, String nodeExecutionId);
}
