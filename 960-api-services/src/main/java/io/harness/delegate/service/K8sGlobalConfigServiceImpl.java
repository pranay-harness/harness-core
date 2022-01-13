/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.service;

import static io.harness.k8s.model.HelmVersion.V2;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.configuration.InstallUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.K8sGlobalConfigService;
import io.harness.k8s.model.HelmVersion;

import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class K8sGlobalConfigServiceImpl implements K8sGlobalConfigService {
  @Override
  public String getKubectlPath(boolean useNewKubectlVersion) {
    return useNewKubectlVersion ? InstallUtils.getNewKubectlPath() : InstallUtils.getDefaultKubectlPath();
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
  public String getChartMuseumPath(boolean useLatestVersion) {
    return InstallUtils.getChartMuseumPath(useLatestVersion);
  }

  @Override
  public String getOcPath() {
    return InstallUtils.getOcPath();
  }

  @Override
  public String getKustomizePath(boolean useLatestVersion) {
    return InstallUtils.getKustomizePath(useLatestVersion);
  }

  @Override
  public String getScmPath() {
    return InstallUtils.getScmPath();
  }
}
