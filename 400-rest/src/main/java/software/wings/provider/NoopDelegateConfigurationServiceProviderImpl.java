package software.wings.provider;

import io.harness.delegate.DelegateConfigurationServiceProvider;

public class NoopDelegateConfigurationServiceProviderImpl implements DelegateConfigurationServiceProvider {
  @Override
  public String getAccount() {
    return "";
  }
}
