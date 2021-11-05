package io.harness.enforcement.executions;

import io.harness.enforcement.beans.metadata.RateLimitRestrictionMetadataDTO;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;
import io.harness.pms.plan.execution.AccountExecutionMetadata;
import io.harness.repositories.executions.AccountExecutionMetadataRepository;

import com.google.inject.Inject;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Optional;

public class CIMonthlyBuildImpl implements RestrictionUsageInterface<RateLimitRestrictionMetadataDTO> {
  @Inject AccountExecutionMetadataRepository accountExecutionMetadataRepository;
  private static String moduleName = "ci_private_build";

  @Override
  public long getCurrentValue(String accountIdentifier, RateLimitRestrictionMetadataDTO restrictionMetadataDTO) {
    Optional<AccountExecutionMetadata> accountExecutionMetadata =
        accountExecutionMetadataRepository.findByAccountId(accountIdentifier);
    if (accountExecutionMetadata.isPresent()) {
      LocalDate startDate = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate();
      YearMonth yearMonth = YearMonth.of(startDate.getYear(), startDate.getMonth());
      return accountExecutionMetadata.get()
          .getModuleToExecutionInfoMap()
          .get(moduleName)
          .getCountPerMonth()
          .getOrDefault(yearMonth.toString(), 0L);
    }
    return 0;
  }
}
