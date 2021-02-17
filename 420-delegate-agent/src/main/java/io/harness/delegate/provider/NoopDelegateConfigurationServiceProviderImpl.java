package io.harness.delegate.provider;

import io.harness.delegate.DelegateConfigurationServiceProvider;

public class NoopDelegateConfigurationServiceProviderImpl implements DelegateConfigurationServiceProvider {
  @Override
  public String getAccount() {
    return new String();
  }
}
