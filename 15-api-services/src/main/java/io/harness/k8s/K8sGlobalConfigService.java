package io.harness.k8s;

import io.harness.k8s.model.HelmVersion;

public interface K8sGlobalConfigService {
  String getKubectlPath();
  String getGoTemplateClientPath();
  String getHelmPath(HelmVersion helmVersion);
  String getChartMuseumPath();
  String getOcPath();
  String getKustomizePath();
}
