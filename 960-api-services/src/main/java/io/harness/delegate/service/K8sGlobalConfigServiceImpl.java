/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.service;

import static io.harness.k8s.model.HelmVersion.V2;

import io.harness.delegate.configuration.InstallUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.K8sGlobalConfigService;
import io.harness.k8s.model.HelmVersion;

import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

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
      log.error("Did not expect null value of helmVersion, defaulting to V2");
      helmVersion = V2;
    }
    log.info("[HELM]: picked helm binary corresponding to version {}", helmVersion);
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

  @Override
  public String getScmPath() {
    return InstallUtils.getScmPath();
  }
}
