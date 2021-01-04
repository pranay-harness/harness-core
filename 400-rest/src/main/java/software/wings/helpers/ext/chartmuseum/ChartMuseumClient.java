package software.wings.helpers.ext.chartmuseum;

import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.settings.SettingValue;

import org.zeroturnaround.exec.StartedProcess;

public interface ChartMuseumClient {
  ChartMuseumServer startChartMuseumServer(HelmRepoConfig helmRepoConfig, SettingValue connectorConfig,
      String resourceDirectory, String basePath) throws Exception;

  void stopChartMuseumServer(StartedProcess process) throws Exception;
}
