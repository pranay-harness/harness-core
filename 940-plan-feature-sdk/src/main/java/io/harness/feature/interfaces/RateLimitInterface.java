package io.harness.feature.interfaces;

public interface RateLimitInterface {
  long getCurrentValue(String accountIdentifier, long startTime, long endTime);
}
