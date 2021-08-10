package io.harness.feature.test;

import io.harness.feature.bases.FeatureLimit;

public class LimitATest extends FeatureLimit {
  @Override
  public Long getUsage(String accountIdentifier) {
    return 10000L;
  }
}
