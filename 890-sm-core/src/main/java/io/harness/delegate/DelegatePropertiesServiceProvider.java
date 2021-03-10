package io.harness.delegate;

import io.harness.managerclient.GetDelegatePropertiesRequest;
import io.harness.managerclient.GetDelegatePropertiesResponse;

public interface DelegatePropertiesServiceProvider {
  GetDelegatePropertiesResponse getDelegateProperties(GetDelegatePropertiesRequest request);
}
