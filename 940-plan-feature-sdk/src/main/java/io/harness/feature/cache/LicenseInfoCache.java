/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.feature.cache;

import io.harness.ModuleType;
import io.harness.licensing.beans.summary.LicensesWithSummaryDTO;

public interface LicenseInfoCache {
  <T extends LicensesWithSummaryDTO> T getLicenseInfo(String accountIdentifier, ModuleType moduleType);
}
