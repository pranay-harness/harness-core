package io.harness.repositories;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CI)
public interface CIAccountExecutionMetadataRepositoryCustom {
  void updateAccountExecutionMetadata(String accountId, Long endTs);
}
