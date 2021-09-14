package io.harness.feature.client.example;

import io.harness.feature.client.usage.PlanFeatureUsageInterface;

import com.google.inject.Singleton;

@Singleton
public class ExampleUsageImpl implements PlanFeatureUsageInterface {
  @Override
  public long getCurrentValue(String accountIdentifier) {
    return 10;
  }
}
