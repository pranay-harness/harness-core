package software.wings.provider;

import io.harness.delegate.DelegatePropertiesServiceProvider;
import io.harness.managerclient.GetDelegatePropertiesRequest;
import io.harness.managerclient.GetDelegatePropertiesResponse;

public class NoopDelegatePropertiesServiceProviderImpl implements DelegatePropertiesServiceProvider {
  @Override
  public GetDelegatePropertiesResponse getDelegateProperties(GetDelegatePropertiesRequest request) {
    return null;
  }
}
