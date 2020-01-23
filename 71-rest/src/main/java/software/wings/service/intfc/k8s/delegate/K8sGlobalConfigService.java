package software.wings.service.intfc.k8s.delegate;

public interface K8sGlobalConfigService {
  String getKubectlPath();
  String getGoTemplateClientPath();
  String getHelmPath();
  String getChartMuseumPath();
  String getOcPath();
}