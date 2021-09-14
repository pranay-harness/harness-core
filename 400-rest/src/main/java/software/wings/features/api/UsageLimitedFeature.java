/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.features.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._950_FEATURE_FLAG)
@OwnedBy(PL)
public interface UsageLimitedFeature extends RestrictedFeature {
  int getMaxUsageAllowedForAccount(String accountId);

  int getMaxUsageAllowed(String accountType);

  int getUsage(String accountId);
}
