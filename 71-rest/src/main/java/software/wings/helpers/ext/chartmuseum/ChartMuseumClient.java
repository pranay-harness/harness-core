package software.wings.helpers.ext.chartmuseum;

import org.zeroturnaround.exec.StartedProcess;
import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.settings.SettingValue;

public interface ChartMuseumClient {
  ChartMuseumServerConfig startChartMuseumServer(HelmRepoConfig helmRepoConfig, SettingValue connectorConfig)
      throws Exception;

  void stopChartMuseumServer(StartedProcess process) throws Exception;
}
