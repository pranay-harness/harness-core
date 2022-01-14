package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.services.api.FeatureFlagService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AlwaysFalseFeatureFlagServiceImpl implements FeatureFlagService {
  @Override
  public boolean isFeatureFlagEnabled(String accountId, String name) {
    return false;
  }
}
