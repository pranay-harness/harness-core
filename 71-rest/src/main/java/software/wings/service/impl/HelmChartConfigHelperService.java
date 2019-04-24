package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.HelmChartConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams.HelmChartConfigParamsBuilder;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.sm.ExecutionContext;

import java.util.List;

@Singleton
public class HelmChartConfigHelperService {
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;

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

  public HelmChartConfigParams getHelmChartConfigTaskParams(
      ExecutionContext context, ApplicationManifest applicationManifest) {
    HelmChartConfig helmChartConfig = applicationManifest.getHelmChartConfig();
    if (helmChartConfig == null) {
      return null;
    }

    String connectorId = helmChartConfig.getConnectorId();
    SettingAttribute settingAttribute = settingsService.get(connectorId);
    notNullCheck("Helm repo config not found with id " + connectorId, settingAttribute);

    HelmRepoConfig helmRepoConfig = (HelmRepoConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptionDataDetails =
        secretManager.getEncryptionDetails(helmRepoConfig, context.getAppId(), null);

    String repoName = generateRepoName(helmRepoConfig, settingAttribute.getUuid(), context.getWorkflowExecutionId());

    HelmChartConfigParamsBuilder helmChartConfigParamsBuilder = HelmChartConfigParams.builder()
                                                                    .chartName(helmChartConfig.getChartName())
                                                                    .chartVersion(helmChartConfig.getChartVersion())
                                                                    .helmRepoConfig(helmRepoConfig)
                                                                    .encryptedDataDetails(encryptionDataDetails)
                                                                    .repoDisplayName(settingAttribute.getName())
                                                                    .repoName(repoName);

    if (isNotBlank(helmRepoConfig.getConnectorId())) {
      SettingAttribute connectorSettingAttribute = settingsService.get(helmRepoConfig.getConnectorId());
      notNullCheck(
          format("Parent connector with id %s not found for helm repo config", helmRepoConfig.getConnectorId()),
          settingAttribute);

      SettingValue value = connectorSettingAttribute.getValue();
      List<EncryptedDataDetail> connectorEncryptedDataDetails =
          secretManager.getEncryptionDetails((EncryptableSetting) value, context.getAppId(), null);

      helmChartConfigParamsBuilder.connectorConfig(value).connectorEncryptedDataDetails(connectorEncryptedDataDetails);
    }

    return helmChartConfigParamsBuilder.build();
  }

  private String generateRepoName(
      HelmRepoConfig helmRepoConfig, String settingAttributeId, String workflowExecutionId) {
    switch (helmRepoConfig.getSettingType()) {
      case HTTP_HELM_REPO:
        return convertBase64UuidToCanonicalForm(settingAttributeId);

      default:
        return convertBase64UuidToCanonicalForm(workflowExecutionId);
    }
  }
}
