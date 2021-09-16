package io.harness.feature.example;

import io.harness.feature.interfaces.RateLimitInterface;

import com.google.inject.Singleton;

@Singleton
public class RateLimitExampleImpl implements RateLimitInterface {
  @Override
  public long getCurrentValue(String accountIdentifier, long startTime, long endTime) {
    return 20;
  }
}
