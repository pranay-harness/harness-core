package io.harness.licensing.usage.interfaces;

import io.harness.ModuleType;
import io.harness.licensing.usage.beans.LicenseUsageDTO;
import io.harness.licensing.usage.params.UsageRequestParams;

public interface LicenseUsageInterface<T extends LicenseUsageDTO, K extends UsageRequestParams> {
  T getLicenseUsage(String accountIdentifier, ModuleType module, long timestamp, K usageRequest);
}
