package io.harness.feature.interfaces;

import io.harness.feature.services.impl.FeatureUsageClient;

public interface LimitRestriction {
  Long getLimit();
  String getClientName();
  FeatureUsageClient getFeatureUsageClient();
  void setFeatureUsageClient(FeatureUsageClient featureUsageClient);
}
