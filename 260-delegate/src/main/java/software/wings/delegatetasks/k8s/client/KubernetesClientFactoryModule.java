package software.wings.delegatetasks.k8s.client;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import com.google.inject.AbstractModule;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class KubernetesClientFactoryModule extends AbstractModule {
  private static KubernetesClientFactoryModule instance;

  private KubernetesClientFactoryModule() {}

  public static KubernetesClientFactoryModule getInstance() {
    if (instance == null) {
      instance = new KubernetesClientFactoryModule();
    }

    return instance;
  }

  @Override
  protected void configure() {
    bind(KubernetesClientFactory.class).to(HarnessKubernetesClientFactory.class);
  }
}
