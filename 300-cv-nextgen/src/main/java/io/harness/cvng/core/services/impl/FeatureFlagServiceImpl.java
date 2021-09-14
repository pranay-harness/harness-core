/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.client.RequestExecutor;
import io.harness.cvng.client.VerificationManagerClient;
import io.harness.cvng.core.services.api.FeatureFlagService;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
@OwnedBy(HarnessTeam.CV)
public class FeatureFlagServiceImpl implements FeatureFlagService {
  @Inject private VerificationManagerClient verificationManagerClient;
  @Inject private RequestExecutor requestExecutor;
  @Override
  public boolean isFeatureFlagEnabled(String accountId, String featureFlagName) {
    Preconditions.checkNotNull(accountId);
    Preconditions.checkNotNull(featureFlagName);
    return requestExecutor.execute(verificationManagerClient.isFeatureEnabled(featureFlagName, accountId))
        .getResource();
  }
}
