package software.wings.service.impl.yaml;

import io.harness.k8s.K8sGlobalConfigService;
import io.harness.k8s.model.HelmVersion;

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
  public String getHelmPath(HelmVersion helmVersion) {
    throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MSG);
  }

  @Override
  public String getChartMuseumPath(boolean useLatestVersion) {
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

  @Override
  public String getScmPath() {
    throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MSG);
  }

  @Override
  public String getHelmVersion(HelmVersion helmVersion) {
    throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MSG);
  }

  @Override
  public String getKubectlVersion() {
    throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MSG);
  }
}
