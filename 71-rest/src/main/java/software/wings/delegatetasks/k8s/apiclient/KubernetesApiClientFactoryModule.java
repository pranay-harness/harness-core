package software.wings.delegatetasks.k8s.apiclient;

import io.harness.k8s.apiclient.ApiClientFactory;
import io.harness.k8s.apiclient.ApiClientFactoryImpl;

import com.google.inject.AbstractModule;

public class KubernetesApiClientFactoryModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(ApiClientFactory.class).to(ApiClientFactoryImpl.class);
  }
}
