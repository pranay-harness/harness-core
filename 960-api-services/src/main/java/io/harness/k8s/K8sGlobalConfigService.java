package io.harness.k8s;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.model.HelmVersion;

@OwnedBy(HarnessTeam.CDP)
public interface K8sGlobalConfigService {
  String getKubectlPath(boolean useNewKubectlVersion);
  String getGoTemplateClientPath();
  String getHelmPath(HelmVersion helmVersion);
  String getChartMuseumPath(boolean useLatestVersion);
  String getOcPath();
  String getKustomizePath(boolean useLatestVersion);
  String getScmPath();
}
