/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
