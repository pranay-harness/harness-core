package software.wings.delegate.service;

import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;

public class K8sGlobalConfigServiceImpl implements K8sGlobalConfigService {
  @Override
  public String getKubectlPath() {
    return InstallUtils.getKubectlPath();
  }

  @Override
  public String getGoTemplateClientPath() {
    // ToDo: Install go-template on delegates and use that path.
    return "D:\\git\\harness\\go-template\\go-template";
  }
}
