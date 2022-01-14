package io.harness.enforcement.executions;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.executions.CDAccountExecutionMetadata;
import io.harness.enforcement.beans.metadata.RateLimitRestrictionMetadataDTO;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;
import io.harness.repositories.executions.CDAccountExecutionMetadataRepository;

import com.google.inject.Inject;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Optional;

@OwnedBy(HarnessTeam.PIPELINE)
public class DeploymentsPerMonthRestrictionUsageImpl
    implements RestrictionUsageInterface<RateLimitRestrictionMetadataDTO> {
  @Inject CDAccountExecutionMetadataRepository accountExecutionMetadataRepository;

  @Override
  public long getCurrentValue(String accountIdentifier, RateLimitRestrictionMetadataDTO restrictionMetadataDTO) {
    Optional<CDAccountExecutionMetadata> accountExecutionMetadata =
        accountExecutionMetadataRepository.findByAccountId(accountIdentifier);
    if (!accountExecutionMetadata.isPresent() || accountExecutionMetadata.get().getExecutionCount() <= 1100) {
      return 0;
    }
    LocalDate startDate = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate();
    YearMonth yearMonth = YearMonth.of(startDate.getYear(), startDate.getMonth());
    return accountExecutionMetadata.get().getAccountExecutionInfo().getCountPerMonth().getOrDefault(
        yearMonth.toString(), 0L);
  }
}
