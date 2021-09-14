/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.provider;

import io.harness.delegate.DelegateConfigurationServiceProvider;

public class NoopDelegateConfigurationServiceProviderImpl implements DelegateConfigurationServiceProvider {
  @Override
  public String getAccount() {
    return "";
  }
}
