package software.wings.service.impl.yaml;

import software.wings.helpers.ext.helm.HelmConstants;
import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;

public class K8sGlobalConfigServiceUnsupported implements K8sGlobalConfigService {
  private static final String UNSUPPORTED_OPERATION_MSG = "K8sGlobalConfigService not available in manager";

  @Override
  public String getKubectlPath() {
    throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MSG);
  }

  @Override
  public String getGoTemplateClientPath() {
    throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MSG);
  }

  @Override
  public String getHelmPath(HelmConstants.HelmVersion helmVersion) {
    throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MSG);
  }

  @Override
  public String getChartMuseumPath() {
    throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MSG);
  }

  @Override
  public String getOcPath() {
    throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MSG);
  }

  @Override
  public String getKustomizePath() {
    throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MSG);
  }
}
