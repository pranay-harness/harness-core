/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms;

import io.harness.beans.FeatureName;

public class NoopFeatureFlagServiceImpl implements PmsFeatureFlagService {
  @Override
  public boolean isEnabled(String accountId, FeatureName featureName) {
    return false;
  }

  @Override
  public boolean isEnabled(String accountId, String featureName) {
    return false;
  }
}
