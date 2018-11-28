package software.wings.delegate.service;

import com.google.inject.Provider;

import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;

public class K8sGlobalConfigServiceFactory implements Provider<K8sGlobalConfigService> {
  @Override
  public K8sGlobalConfigService get() {
    return new K8sGlobalConfigServiceImpl();
  }
}
