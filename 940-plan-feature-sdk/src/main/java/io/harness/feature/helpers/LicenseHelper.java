package io.harness.feature.helpers;

import io.harness.ModuleType;
import io.harness.licensing.beans.summary.LicensesWithSummaryDTO;

import java.util.Optional;

public interface LicenseHelper {
  Optional<LicensesWithSummaryDTO> getLicenseSummary(String accountIdentifier, ModuleType moduleType);
}
