package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import software.wings.beans.HelmChartConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.SettingsService;

@Singleton
public class HelmChartConfigHelperService {
  @Inject private SettingsService settingsService;

  public HelmChartConfig getHelmChartConfigFromYaml(String accountId, String appId, HelmChartConfig helmChartConfig) {
    if (helmChartConfig == null) {
      return null;
    }

    HelmChartConfig newHelmChartConfig = createHelmChartConfig(helmChartConfig);

    SettingAttribute settingAttribute =
        settingsService.getByName(accountId, appId, newHelmChartConfig.getConnectorName());
    if (settingAttribute == null) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "Helm repository does not exist with name " + newHelmChartConfig.getConnectorName());
    }

    newHelmChartConfig.setConnectorId(settingAttribute.getUuid());
    newHelmChartConfig.setConnectorName(null);

    return newHelmChartConfig;
  }

  public HelmChartConfig getHelmChartConfigForToYaml(HelmChartConfig helmChartConfig) {
    if (helmChartConfig == null) {
      return null;
    }

    HelmChartConfig newHelmChartConfig = createHelmChartConfig(helmChartConfig);

    SettingAttribute settingAttribute = settingsService.get(newHelmChartConfig.getConnectorId());
    if (settingAttribute == null) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "Helm repository does not exist with id " + newHelmChartConfig.getConnectorId());
    }

    newHelmChartConfig.setConnectorId(null);
    newHelmChartConfig.setConnectorName(settingAttribute.getName());

    return newHelmChartConfig;
  }

  private HelmChartConfig createHelmChartConfig(HelmChartConfig helmChartConfig) {
    return HelmChartConfig.builder()
        .connectorId(helmChartConfig.getConnectorId())
        .chartName(helmChartConfig.getChartName())
        .chartVersion(helmChartConfig.getChartVersion())
        .connectorName(helmChartConfig.getConnectorName())
        .build();
  }
}
