package io.harness.enforcement.client.example;

import io.harness.enforcement.client.usage.RestrictionUsageInterface;

import com.google.inject.Singleton;

@Singleton
public class ExampleUsageImpl implements RestrictionUsageInterface {
  @Override
  public long getCurrentValue(String accountIdentifier) {
    return 10;
  }
}
