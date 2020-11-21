package io.harness.cvng.core.services.impl;

import io.harness.cvng.client.VerificationManagerClient;
import io.harness.cvng.core.services.api.FeatureFlagService;

import com.google.inject.Inject;
import java.io.IOException;

public class FeatureFlagServiceImpl implements FeatureFlagService {
  @Inject VerificationManagerClient verificationManagerClient;
  @Override
  public boolean isFeatureFlagEnabled(String name, String accountId) {
    try {
      // TODO: just added this to check the end to end integration with manager. We need to refactor this and handle
      // manager
      //  calls in a single place with more standard way with proper exception handling and stacktrace.
      return verificationManagerClient.isFeatureEnabled(name, accountId).execute().body().getResource();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
