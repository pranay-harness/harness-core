package io.harness.feature.client.services.impl;

import static io.harness.remote.client.NGRestUtils.getResponse;

import io.harness.feature.client.PlanFeatureClient;
import io.harness.feature.client.PlanFeatureClientConfiguration;
import io.harness.feature.client.services.PlanFeatureClientService;
import io.harness.feature.client.services.PlanFeatureRegisterService;
import io.harness.feature.client.usage.PlanFeatureUsageInterface;
import io.harness.feature.constants.FeatureRestriction;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PlanFeatureClientServiceImpl implements PlanFeatureClientService {
  private final PlanFeatureClient planFeatureClient;
  private final PlanFeatureRegisterService planFeatureRegisterService;
  private final PlanFeatureClientConfiguration planFeatureClientConfiguration;

  @Inject
  public PlanFeatureClientServiceImpl(PlanFeatureClient planFeatureClient,
      PlanFeatureRegisterService planFeatureRegisterService,
      PlanFeatureClientConfiguration planFeatureClientConfiguration) {
    this.planFeatureClient = planFeatureClient;
    this.planFeatureRegisterService = planFeatureRegisterService;
    this.planFeatureClientConfiguration = planFeatureClientConfiguration;
  }

  @Override
  public boolean isAvailable(FeatureRestriction featureName, String accountIdentifier) {
    try {
      checkAvailability(featureName, accountIdentifier);
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  @Override
  public void checkAvailability(FeatureRestriction featureName, String accountIdentifier) {
    if (!planFeatureClientConfiguration.isPlanFeatureCheckEnabled()) {
      return;
    }

    PlanFeatureUsageInterface planFeatureUsage = planFeatureRegisterService.get(featureName);

    long currentUsage = 0;
    if (planFeatureUsage != null) {
      currentUsage = planFeatureUsage.getCurrentValue(accountIdentifier);
    }

    getResponse(planFeatureClient.checkFeatureAvailability(featureName, accountIdentifier, currentUsage));
  }
}
