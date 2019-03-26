package software.wings.delegate.service;

import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;

public class K8sGlobalConfigServiceImpl implements K8sGlobalConfigService {
  @Override
  public String getKubectlPath() {
    return InstallUtils.getKubectlPath();
  }

  @Override
  public String getGoTemplateClientPath() {
    return InstallUtils.getGoTemplateToolPath();
  }

  @Override
  public String getHelmPath() {
    return InstallUtils.getHelmPath();
  }
}
