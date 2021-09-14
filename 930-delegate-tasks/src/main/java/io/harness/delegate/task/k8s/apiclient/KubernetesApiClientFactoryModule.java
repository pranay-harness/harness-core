/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.k8s.apiclient;

import io.harness.k8s.apiclient.ApiClientFactory;
import io.harness.k8s.apiclient.ApiClientFactoryImpl;

import com.google.inject.AbstractModule;

public class KubernetesApiClientFactoryModule extends AbstractModule {
  private static KubernetesApiClientFactoryModule instance;

  private KubernetesApiClientFactoryModule() {}

  public static KubernetesApiClientFactoryModule getInstance() {
    if (instance == null) {
      instance = new KubernetesApiClientFactoryModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(ApiClientFactory.class).to(ApiClientFactoryImpl.class);
  }
}
