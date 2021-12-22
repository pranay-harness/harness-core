package io.harness.engine;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.governance.GovernanceMetadata;

@OwnedBy(HarnessTeam.PIPELINE)
public interface GovernanceService {
  GovernanceMetadata evaluateGovernancePolicies(String expandedJson, String accountId, String orgIdentifier,
      String projectIdentifier, String action, String planExecutionId);
}
