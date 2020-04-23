package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.context.ContextElementType;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.PhaseElement;
import software.wings.beans.HelmChartConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.helpers.ext.helm.HelmConstants;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams.HelmChartConfigParamsBuilder;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.sm.ExecutionContext;

import java.util.List;

@Singleton
public class HelmChartConfigHelperService {
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private ServiceResourceService serviceResourceService;

  public HelmChartConfig getHelmChartConfigFromYaml(String accountId, String appId, HelmChartConfig helmChartConfig) {
    if (helmChartConfig == null) {
      return null;
    }

    HelmChartConfig newHelmChartConfig = createHelmChartConfig(helmChartConfig);

    if (isNotBlank(newHelmChartConfig.getConnectorName())) {
      SettingAttribute settingAttribute =
          settingsService.getByName(accountId, appId, newHelmChartConfig.getConnectorName());
      if (settingAttribute == null) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT)
            .addParam("args", "Helm repository does not exist with name " + newHelmChartConfig.getConnectorName());
      }

      newHelmChartConfig.setConnectorId(settingAttribute.getUuid());
      newHelmChartConfig.setConnectorName(null);
    }

    return newHelmChartConfig;
  }

  public HelmChartConfig getHelmChartConfigForToYaml(HelmChartConfig helmChartConfig) {
    if (helmChartConfig == null) {
      return null;
    }

    HelmChartConfig newHelmChartConfig = createHelmChartConfig(helmChartConfig);

    if (isNotBlank(newHelmChartConfig.getConnectorId())) {
      SettingAttribute settingAttribute = settingsService.get(newHelmChartConfig.getConnectorId());
      if (settingAttribute == null) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT)
            .addParam("args", "Helm repository does not exist with id " + newHelmChartConfig.getConnectorId());
      }

      newHelmChartConfig.setConnectorId(null);
      newHelmChartConfig.setConnectorName(settingAttribute.getName());
    }

    return newHelmChartConfig;
  }

  private HelmChartConfig createHelmChartConfig(HelmChartConfig helmChartConfig) {
    return HelmChartConfig.builder()
        .connectorId(helmChartConfig.getConnectorId())
        .chartName(helmChartConfig.getChartName())
        .chartVersion(helmChartConfig.getChartVersion())
        .connectorName(helmChartConfig.getConnectorName())
        .chartUrl(helmChartConfig.getChartUrl())
        .basePath(helmChartConfig.getBasePath())
        .build();
  }

  public HelmChartConfigParams getHelmChartConfigTaskParams(
      ExecutionContext context, ApplicationManifest applicationManifest) {
    HelmChartConfig helmChartConfig = applicationManifest.getHelmChartConfig();
    if (helmChartConfig == null) {
      return null;
    }

    if (isNotBlank(helmChartConfig.getChartUrl())) {
      helmChartConfig.setChartUrl(context.renderExpression(helmChartConfig.getChartUrl()));
    }

    if (isNotBlank(helmChartConfig.getChartName())) {
      helmChartConfig.setChartName(context.renderExpression(helmChartConfig.getChartName()));
    }

    if (isNotBlank(helmChartConfig.getChartVersion())) {
      helmChartConfig.setChartVersion(context.renderExpression(helmChartConfig.getChartVersion()));
    }

    HelmChartConfigParamsBuilder helmChartConfigParamsBuilder =
        HelmChartConfigParams.builder().chartVersion(helmChartConfig.getChartVersion());

    if (isNotBlank(helmChartConfig.getChartName())) {
      String chartName = helmChartConfig.getChartName();
      int lastIndex = chartName.lastIndexOf('/');

      if (lastIndex != -1) {
        helmChartConfigParamsBuilder.chartName(chartName.substring(lastIndex + 1))
            .repoName(chartName.substring(0, lastIndex));
      } else {
        helmChartConfigParamsBuilder.chartName(chartName);
      }
    }

    if (isNotBlank(helmChartConfig.getChartUrl())) {
      helmChartConfigParamsBuilder.chartUrl(helmChartConfig.getChartUrl())
          .repoName(convertBase64UuidToCanonicalForm(context.getWorkflowExecutionId()));
    }

    helmChartConfigParamsBuilder.helmVersion(getHelmVersionFromService(context));

    String connectorId = helmChartConfig.getConnectorId();
    if (isBlank(connectorId)) {
      return helmChartConfigParamsBuilder.build();
    }

    SettingAttribute settingAttribute = settingsService.get(connectorId);
    notNullCheck("Helm repo config not found with id " + connectorId, settingAttribute);

    HelmRepoConfig helmRepoConfig = (HelmRepoConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptionDataDetails =
        secretManager.getEncryptionDetails(helmRepoConfig, context.getAppId(), null);

    String repoName = generateRepoName(helmRepoConfig, settingAttribute.getUuid(), context.getWorkflowExecutionId());

    helmChartConfigParamsBuilder.helmRepoConfig(helmRepoConfig)
        .encryptedDataDetails(encryptionDataDetails)
        .repoDisplayName(settingAttribute.getName())
        .repoName(repoName)
        .basePath(context.renderExpression(helmChartConfig.getBasePath()));

    if (isNotBlank(helmRepoConfig.getConnectorId())) {
      SettingAttribute connectorSettingAttribute = settingsService.get(helmRepoConfig.getConnectorId());
      notNullCheck(format("Cloud provider deleted for helm repository connector [%s] selected in service",
                       settingAttribute.getName()),
          connectorSettingAttribute);

      SettingValue value = connectorSettingAttribute.getValue();
      List<EncryptedDataDetail> connectorEncryptedDataDetails =
          secretManager.getEncryptionDetails((EncryptableSetting) value, context.getAppId(), null);

      helmChartConfigParamsBuilder.connectorConfig(value).connectorEncryptedDataDetails(connectorEncryptedDataDetails);
    }

    return helmChartConfigParamsBuilder.build();
  }

  private HelmConstants.HelmVersion getHelmVersionFromService(ExecutionContext context) {
    return serviceResourceService.getHelmVersionWithDefault(context.getAppId(),
        ((PhaseElement) context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM))
            .getServiceElement()
            .getUuid());
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
