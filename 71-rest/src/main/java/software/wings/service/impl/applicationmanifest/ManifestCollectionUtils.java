package software.wings.service.impl.applicationmanifest;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.manifests.request.ManifestCollectionParams;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.model.HelmVersion;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.tasks.Cd1SetupFields;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.HelmChartConfig;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.HelmChart;
import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.beans.settings.helm.HelmRepoConfigValidationTaskParams;
import software.wings.helpers.ext.helm.request.HelmChartCollectionParams;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams.HelmChartConfigParamsBuilder;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.applicationmanifest.HelmChartService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@OwnedBy(CDC)
public class ManifestCollectionUtils {
  @Inject ApplicationManifestService applicationManifestService;
  @Inject AppService appService;
  @Inject SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private HelmChartService helmChartService;
  @Inject private ServiceResourceService serviceResourceService;

  public ManifestCollectionParams prepareCollectTaskParams(String appManifestId, String appId) {
    ApplicationManifest appManifest = applicationManifestService.getById(appId, appManifestId);
    if (appManifest == null || !Boolean.TRUE.equals(appManifest.getPollForChanges())) {
      throw new InvalidRequestException("Collection not configured for app manifest");
    }
    String accountId =
        appManifest.getAccountId() != null ? appManifest.getAccountId() : appService.getAccountIdByAppId(appId);

    Service service = serviceResourceService.get(appId, appManifest.getServiceId());
    if (service == null) {
      throw new InvalidRequestException("Service not found for the application manifest");
    }
    HelmChartConfig helmChartConfig = appManifest.getHelmChartConfig();

    HelmChartConfigParamsBuilder helmChartConfigParamsBuilder =
        constructHelmChartConfigParamsBuilder(appId, helmChartConfig, service.getHelmVersion());

    return HelmChartCollectionParams.builder()
        .accountId(accountId)
        .appId(appId)
        .appManifestId(appManifestId)
        .serviceId(appManifest.getServiceId())
        .publishedVersions(getPublishedVersionsForAppManifest(accountId, appManifestId))
        .helmChartConfigParams(helmChartConfigParamsBuilder.build())
        .build();
  }

  private Set<String> getPublishedVersionsForAppManifest(String accountId, String appManifestId) {
    List<HelmChart> helmCharts = helmChartService.listHelmChartsForAppManifest(accountId, appManifestId);
    return helmCharts == null ? new HashSet<>()
                              : helmCharts.stream().map(HelmChart::getVersion).collect(Collectors.toSet());
  }

  private HelmChartConfigParamsBuilder constructHelmChartConfigParamsBuilder(
      String appId, HelmChartConfig helmChartConfig, HelmVersion helmVersion) {
    SettingAttribute settingAttribute = settingsService.get(helmChartConfig.getConnectorId());
    notNullCheck("Helm repo config not found with id " + helmChartConfig.getConnectorId(), settingAttribute);

    HelmRepoConfig helmRepoConfig = (HelmRepoConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptionDataDetails = secretManager.getEncryptionDetails(helmRepoConfig, appId, null);

    HelmChartConfigParamsBuilder helmChartConfigParamsBuilder =
        HelmChartConfigParams.builder()
            .chartName(helmChartConfig.getChartName())
            .chartUrl(helmChartConfig.getChartUrl())
            .basePath(helmChartConfig.getBasePath())
            .encryptedDataDetails(encryptionDataDetails)
            .repoName(convertBase64UuidToCanonicalForm(settingAttribute.getUuid()))
            .repoDisplayName(settingAttribute.getName())
            .helmVersion(helmVersion)
            .helmRepoConfig(helmRepoConfig);

    if (isNotBlank(helmRepoConfig.getConnectorId())) {
      SettingAttribute connectorSettingAttribute = settingsService.get(helmRepoConfig.getConnectorId());
      notNullCheck(format("Cloud provider deleted for helm repository connector [%s] selected in service",
                       settingAttribute.getName()),
          connectorSettingAttribute);

      SettingValue settingValue = connectorSettingAttribute.getValue();
      List<EncryptedDataDetail> connectorEncryptedDataDetails =
          secretManager.getEncryptionDetails((EncryptableSetting) settingValue, appId, null);

      helmChartConfigParamsBuilder.connectorConfig(settingValue)
          .connectorEncryptedDataDetails(connectorEncryptedDataDetails);
    }
    return helmChartConfigParamsBuilder;
  }

  public DelegateTask buildValidationTask(String appManifestId, String appId) {
    HelmChartCollectionParams helmChartCollectionParams =
        (HelmChartCollectionParams) prepareCollectTaskParams(appManifestId, appId);
    HelmRepoConfigValidationTaskParams helmRepoConfigValidationTaskParams =
        HelmRepoConfigValidationTaskParams.builder()
            .appId(appId)
            .accountId(helmChartCollectionParams.getAccountId())
            .helmRepoConfig(helmChartCollectionParams.getHelmChartConfigParams().getHelmRepoConfig())
            .encryptedDataDetails(helmChartCollectionParams.getHelmChartConfigParams().getEncryptedDataDetails())
            .connectorConfig(helmChartCollectionParams.getHelmChartConfigParams().getConnectorConfig())
            .repoDisplayName(helmChartCollectionParams.getHelmChartConfigParams().getRepoDisplayName())
            .connectorEncryptedDataDetails(
                helmChartCollectionParams.getHelmChartConfigParams().getConnectorEncryptedDataDetails())
            .build();
    return DelegateTask.builder()
        .accountId(helmChartCollectionParams.getAccountId())
        .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, appId)
        .data(TaskData.builder()
                  .async(false)
                  .taskType(TaskType.HELM_REPO_CONFIG_VALIDATION.name())
                  .parameters(new Object[] {helmRepoConfigValidationTaskParams})
                  .timeout(TimeUnit.MINUTES.toMillis(2))
                  .build())
        .build();
  }
}
