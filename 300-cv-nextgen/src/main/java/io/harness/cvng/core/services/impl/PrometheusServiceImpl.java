package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.services.api.OnboardingService;
import io.harness.cvng.core.services.api.PrometheusService;

import com.google.inject.Inject;
import java.util.List;

public class PrometheusServiceImpl implements PrometheusService {
  @Inject OnboardingService onboardingService;
  @Override
  public List<String> getMetricNames(
      String accountId, String connectorIdentifier, String orgIdentifier, String projectIdentifier, String tracingId) {
    return null;
  }

  @Override
  public List<String> getLabelNames(
      String accountId, String connectorIdentifier, String orgIdentifier, String projectIdentifier, String tracingId) {
    return null;
  }

  @Override
  public List<String> getLabelValues(String accountId, String connectorIdentifier, String orgIdentifier,
      String projectIdentifier, String labelName, String tracingId) {
    return null;
  }
}
