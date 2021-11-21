package io.harness.enforcement;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.enforcement.beans.CustomRestrictionEvaluationDTO;
import io.harness.enforcement.client.custom.CustomRestrictionInterface;
import io.harness.licensing.Edition;
import io.harness.pipeline.CIAccountExecutionMetadata;
import io.harness.repositories.CIAccountExecutionMetadataRepository;

import com.google.inject.Inject;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Optional;

@OwnedBy(HarnessTeam.CI)
public class BuildRestrictionUsageImpl implements CustomRestrictionInterface {
  @Inject CIAccountExecutionMetadataRepository accountExecutionMetadataRepository;

  @Override
  public boolean evaluateCustomRestriction(CustomRestrictionEvaluationDTO customFeatureEvaluationDTO) {
    String accountIdentifier = customFeatureEvaluationDTO.getAccountIdentifier();
    Edition edition = customFeatureEvaluationDTO.getEdition();
    if (edition == Edition.FREE) {
      Optional<CIAccountExecutionMetadata> accountExecutionMetadata =
          accountExecutionMetadataRepository.findByAccountId(accountIdentifier);
      if (!accountExecutionMetadata.isPresent() || accountExecutionMetadata.get().getExecutionCount() <= 2500) {
        return true;
      }
      LocalDate startDate = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate();
      YearMonth yearMonth = YearMonth.of(startDate.getYear(), startDate.getMonth());
      return accountExecutionMetadata.get().getAccountExecutionInfo().getCountPerMonth().getOrDefault(
                 yearMonth.toString(), 0L)
          < 100;
    }
    return true;
  }
}
