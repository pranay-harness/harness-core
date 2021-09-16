package io.harness.feature.cache;

import io.harness.ModuleType;
import io.harness.licensing.beans.summary.LicensesWithSummaryDTO;

public interface LicenseInfoCache {
  <T extends LicensesWithSummaryDTO> T getLicenseInfo(String accountIdentifier, ModuleType moduleType);
}
