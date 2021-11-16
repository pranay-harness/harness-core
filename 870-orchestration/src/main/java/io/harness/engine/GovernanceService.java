package io.harness.engine;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.governance.GovernanceMetadata;

@OwnedBy(HarnessTeam.PIPELINE)
public interface GovernanceService {
  GovernanceMetadata evaluateGovernancePolicies(String yaml, String accountId, String action, String planExecutionId);
}
