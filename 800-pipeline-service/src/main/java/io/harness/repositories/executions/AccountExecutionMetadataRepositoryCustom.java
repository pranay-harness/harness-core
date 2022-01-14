package io.harness.repositories.executions;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Set;

@OwnedBy(HarnessTeam.PIPELINE)
public interface AccountExecutionMetadataRepositoryCustom {
  void updateAccountExecutionMetadata(String accountId, Set<String> moduleNames, Long endTs);
}
