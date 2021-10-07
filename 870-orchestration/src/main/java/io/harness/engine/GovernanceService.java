package io.harness.engine;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.pms.contracts.governance.GovernanceMetadata;
import io.harness.pms.contracts.plan.ExecutionMetadata;

import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public interface GovernanceService {
  GovernanceMetadata evaluateGovernancePolicies(ExecutionMetadata executionMetadata,
      PlanExecutionMetadata planExecutionMetadata, Map<String, String> setupAbstractions);
}
