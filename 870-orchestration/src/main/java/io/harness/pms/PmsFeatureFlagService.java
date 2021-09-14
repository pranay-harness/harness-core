/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms;

import io.harness.beans.FeatureName;

import javax.validation.constraints.NotNull;

public interface PmsFeatureFlagService {
  boolean isEnabled(String accountId, @NotNull FeatureName featureName);

  boolean isEnabled(String accountId, @NotNull String featureName);
}
