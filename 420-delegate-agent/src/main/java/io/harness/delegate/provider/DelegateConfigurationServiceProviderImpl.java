/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.provider;

import io.harness.delegate.DelegateConfigurationServiceProvider;
import io.harness.delegate.configuration.DelegateConfiguration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class DelegateConfigurationServiceProviderImpl implements DelegateConfigurationServiceProvider {
  @Inject DelegateConfiguration delegateConfiguration;

  @Override
  public String getAccount() {
    return delegateConfiguration.getAccountId();
  }
}
