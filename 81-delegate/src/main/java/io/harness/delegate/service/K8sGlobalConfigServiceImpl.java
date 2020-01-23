package io.harness.delegate.service;

import io.harness.delegate.configuration.InstallUtils;
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

  @Override
  public String getChartMuseumPath() {
    return InstallUtils.getChartMuseumPath();
  }

  @Override
  public String getOcPath() {
    return InstallUtils.getOcPath();
  }
}
