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
