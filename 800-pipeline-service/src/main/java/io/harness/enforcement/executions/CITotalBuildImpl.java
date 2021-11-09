package io.harness.enforcement.executions;

import io.harness.enforcement.beans.metadata.StaticLimitRestrictionMetadataDTO;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;
import io.harness.pms.plan.execution.AccountExecutionMetadata;
import io.harness.repositories.executions.AccountExecutionMetadataRepository;

import com.google.inject.Inject;
import java.util.Optional;

public class CITotalBuildImpl implements RestrictionUsageInterface<StaticLimitRestrictionMetadataDTO> {
  @Inject AccountExecutionMetadataRepository accountExecutionMetadataRepository;
  private static final String moduleName = "ci_private_build";
  @Override
  public long getCurrentValue(String accountIdentifier, StaticLimitRestrictionMetadataDTO restrictionMetadataDTO) {
    Optional<AccountExecutionMetadata> accountExecutionMetadata =
        accountExecutionMetadataRepository.findByAccountId(accountIdentifier);
    if (accountExecutionMetadata.isPresent()) {
      return accountExecutionMetadata.get().getModuleToExecutionCount().getOrDefault(moduleName, 0L);
    } else {
      return 0;
    }
  }
}
