package io.harness.delegate.service;

import static software.wings.helpers.ext.helm.HelmConstants.HelmVersion.V2;

import io.harness.delegate.configuration.InstallUtils;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.helpers.ext.helm.HelmConstants.HelmVersion;
import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;

import javax.annotation.Nullable;

@Slf4j
public class K8sGlobalConfigServiceImpl implements K8sGlobalConfigService {
  @Override
  public String getKubectlPath() {
    return InstallUtils.getKubectlPath();
  }

  @Override
  public String getGoTemplateClientPath() {
    return InstallUtils.getGoTemplateToolPath();
  }

  /*
  For all helm commands run through the binary installed through InstallUtils, we want to default to v3.
   */
  public String getHelmPath(@Nullable HelmVersion helmVersion) {
    if (helmVersion == null) {
      logger.error("Did not expect null value of helmVersion, defaulting to V2");
      helmVersion = V2;
    }
    logger.info("[HELM]: picked helm binary corresponding to version {}", helmVersion);
    switch (helmVersion) {
      case V2:
        return InstallUtils.getHelm2Path();
      case V3:
        return InstallUtils.getHelm3Path();
      default:
        throw new InvalidRequestException("Unsupported Helm Version:" + helmVersion);
    }
  }

  @Override
  public String getChartMuseumPath() {
    return InstallUtils.getChartMuseumPath();
  }

  @Override
  public String getOcPath() {
    return InstallUtils.getOcPath();
  }

  @Override
  public String getKustomizePath() {
    return InstallUtils.getKustomizePath();
  }
}
