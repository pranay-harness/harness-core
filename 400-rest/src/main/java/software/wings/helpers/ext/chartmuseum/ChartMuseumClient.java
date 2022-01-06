/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.helpers.ext.chartmuseum;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.chartmuseum.ChartMuseumServer;

import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.settings.SettingValue;

import org.zeroturnaround.exec.StartedProcess;

@TargetModule(HarnessModule._960_API_SERVICES)
public interface ChartMuseumClient {
  ChartMuseumServer startChartMuseumServer(HelmRepoConfig helmRepoConfig, SettingValue connectorConfig,
      String resourceDirectory, String basePath, boolean useLatestChartMuseumVersion) throws Exception;

  void stopChartMuseumServer(StartedProcess process) throws Exception;
}
