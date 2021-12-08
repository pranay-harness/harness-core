package io.harness.enforcement;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.enforcement.beans.metadata.RateLimitRestrictionMetadataDTO;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;
import io.harness.repositories.CIAccountExecutionMetadataRepository;

import ci.pipeline.execution.CIAccountExecutionMetadata;
import com.google.inject.Inject;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Optional;

@OwnedBy(HarnessTeam.CI)
public class BuildsPerMonthRestrictionUsageImpl implements RestrictionUsageInterface<RateLimitRestrictionMetadataDTO> {
  @Inject CIAccountExecutionMetadataRepository accountExecutionMetadataRepository;

  @Override
  public long getCurrentValue(String accountIdentifier, RateLimitRestrictionMetadataDTO restrictionMetadataDTO) {
    Optional<CIAccountExecutionMetadata> accountExecutionMetadata =
        accountExecutionMetadataRepository.findByAccountId(accountIdentifier);
    if (!accountExecutionMetadata.isPresent()) {
      return 0;
    }
    LocalDate startDate = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate();
    YearMonth yearMonth = YearMonth.of(startDate.getYear(), startDate.getMonth());
    return accountExecutionMetadata.get().getAccountExecutionInfo().getCountPerMonth().getOrDefault(
        yearMonth.toString(), 0L);
  }
}