package software.wings.delegatetasks.k8s.client;

import com.google.inject.AbstractModule;

public class KubernetesClientFactoryModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(KubernetesClientFactory.class).to(HarnessKubernetesClientFactory.class);
  }
}
