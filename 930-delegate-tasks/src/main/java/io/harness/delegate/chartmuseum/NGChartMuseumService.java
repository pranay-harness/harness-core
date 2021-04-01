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
