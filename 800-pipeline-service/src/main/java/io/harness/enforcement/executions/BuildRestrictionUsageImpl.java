package io.harness.enforcement.executions;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.enforcement.beans.CustomRestrictionEvaluationDTO;
import io.harness.enforcement.client.custom.CustomRestrictionInterface;
import io.harness.licensing.Edition;
import io.harness.pms.plan.execution.AccountExecutionMetadata;
import io.harness.repositories.executions.AccountExecutionMetadataRepository;

import com.google.inject.Inject;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Optional;

@OwnedBy(HarnessTeam.PIPELINE)
public class BuildRestrictionUsageImpl implements CustomRestrictionInterface {
  private static String moduleName = "ci_private_build";
  @Inject AccountExecutionMetadataRepository accountExecutionMetadataRepository;

  @Override
  public boolean evaluateCustomRestriction(CustomRestrictionEvaluationDTO customFeatureEvaluationDTO) {
    String accountIdentifier = customFeatureEvaluationDTO.getAccountIdentifier();
    Edition edition = customFeatureEvaluationDTO.getEdition();
    if (edition == Edition.FREE) {
      Optional<AccountExecutionMetadata> accountExecutionMetadata =
          accountExecutionMetadataRepository.findByAccountId(accountIdentifier);
      if (!accountExecutionMetadata.isPresent()
          || accountExecutionMetadata.get().getModuleToExecutionCount().get(moduleName) <= 2600) {
        return true;
      }
      LocalDate startDate = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate();
      YearMonth yearMonth = YearMonth.of(startDate.getYear(), startDate.getMonth());
      return accountExecutionMetadata.get()
                 .getModuleToExecutionInfoMap()
                 .get(moduleName)
                 .getCountPerMonth()
                 .getOrDefault(yearMonth.toString(), 0L)
          > 100;
    }
    return true;
  }
}