package io.harness.licensing.usage.interfaces;

import io.harness.ModuleType;
import io.harness.licensing.usage.beans.LicenseUsageDTO;

public interface LicenseUsageInterface<T extends LicenseUsageDTO> {
  T getLicenseUsage(String accountIdentifier, ModuleType module, long timestamp);
}
