/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.licensing.usage.interfaces;

import io.harness.ModuleType;
import io.harness.licensing.usage.beans.LicenseUsageDTO;

public interface LicenseUsageInterface<T extends LicenseUsageDTO> {
  T getLicenseUsage(String accountIdentifier, ModuleType module, long timestamp);
}
