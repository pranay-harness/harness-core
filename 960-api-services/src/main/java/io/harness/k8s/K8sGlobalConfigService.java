package io.harness.k8s;

import io.harness.k8s.model.HelmVersion;

public interface K8sGlobalConfigService {
  String getKubectlPath();
  String getGoTemplateClientPath();
  String getHelmPath(HelmVersion helmVersion);
  String getChartMuseumPath(boolean useLatestVersion);
  String getOcPath();
  String getKustomizePath();
  String getScmPath();
}
