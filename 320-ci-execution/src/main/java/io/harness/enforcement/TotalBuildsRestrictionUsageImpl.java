package io.harness.enforcement;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.enforcement.beans.metadata.StaticLimitRestrictionMetadataDTO;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;
import io.harness.repositories.CIAccountExecutionMetadataRepository;

import ci.pipeline.execution.CIAccountExecutionMetadata;
import com.google.inject.Inject;
import java.util.Optional;

@OwnedBy(HarnessTeam.CI)
public class TotalBuildsRestrictionUsageImpl implements RestrictionUsageInterface<StaticLimitRestrictionMetadataDTO> {
  @Inject CIAccountExecutionMetadataRepository accountExecutionMetadataRepository;

  @Override
  public long getCurrentValue(String accountIdentifier, StaticLimitRestrictionMetadataDTO restrictionMetadataDTO) {
    Optional<CIAccountExecutionMetadata> accountExecutionMetadata =
        accountExecutionMetadataRepository.findByAccountId(accountIdentifier);
    if (!accountExecutionMetadata.isPresent()) {
      return 0;
    }
    return accountExecutionMetadata.get().getExecutionCount();
  }
}