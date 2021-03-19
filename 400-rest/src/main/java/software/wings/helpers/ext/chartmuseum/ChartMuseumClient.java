package software.wings.helpers.ext.chartmuseum;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.chartmuseum.ChartMuseumServer;

import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.settings.SettingValue;

import org.zeroturnaround.exec.StartedProcess;

@TargetModule(Module._960_API_SERVICES)
public interface ChartMuseumClient {
  ChartMuseumServer startChartMuseumServer(HelmRepoConfig helmRepoConfig, SettingValue connectorConfig,
      String resourceDirectory, String basePath) throws Exception;

  void stopChartMuseumServer(StartedProcess process) throws Exception;
}
