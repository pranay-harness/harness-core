package software.wings.service.intfc.k8s.delegate;

import software.wings.helpers.ext.helm.HelmConstants.HelmVersion;

public interface K8sGlobalConfigService {
  String getKubectlPath();
  String getGoTemplateClientPath();
  String getHelmPath(HelmVersion helmVersion);
  String getChartMuseumPath();
  String getOcPath();
  String getKustomizePath();
}