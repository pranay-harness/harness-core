/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.chartmuseum;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.chartmuseum.ChartMuseumServer;
import io.harness.delegate.beans.storeconfig.StoreDelegateConfig;

@OwnedBy(CDP)
public interface NGChartMuseumService {
  ChartMuseumServer startChartMuseumServer(StoreDelegateConfig storeDelegateConfig, String resourceDirectory)
      throws Exception;

  void stopChartMuseumServer(ChartMuseumServer chartMuseumServer);
}
