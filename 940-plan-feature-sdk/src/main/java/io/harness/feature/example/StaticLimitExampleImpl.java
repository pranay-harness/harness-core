package io.harness.feature.example;

import io.harness.feature.interfaces.StaticLimitInterface;

import com.google.inject.Singleton;

@Singleton
public class StaticLimitExampleImpl implements StaticLimitInterface {
  @Override
  public long getCurrentValue(String accountIdentifier) {
    return 10;
  }
}
