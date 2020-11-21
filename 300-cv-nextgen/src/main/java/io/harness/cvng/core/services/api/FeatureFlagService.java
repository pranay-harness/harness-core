package io.harness.cvng.core.services.api;

public interface FeatureFlagService {
  boolean isFeatureFlagEnabled(String name, String accountId);
}
