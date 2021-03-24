package io.harness.delegate.chartmuseum;

import io.harness.chartmuseum.ChartMuseumServer;
import io.harness.delegate.beans.storeconfig.StoreDelegateConfig;

public interface NGChartMuseumService {
  ChartMuseumServer startChartMuseumServer(StoreDelegateConfig storeDelegateConfig, String resourceDirectory)
      throws Exception;

  void stopChartMuseumServer(ChartMuseumServer chartMuseumServer);
}
