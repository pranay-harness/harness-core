package io.harness.feature.interfaces;

public interface FeatureUsage<T> {
  T getUsage(String accountIdentifier);
}
