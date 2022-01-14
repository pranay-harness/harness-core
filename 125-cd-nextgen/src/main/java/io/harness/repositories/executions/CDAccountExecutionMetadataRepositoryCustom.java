package io.harness.repositories.executions;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public interface CDAccountExecutionMetadataRepositoryCustom {
  void updateAccountExecutionMetadata(String accountId, Long endTs);
}
