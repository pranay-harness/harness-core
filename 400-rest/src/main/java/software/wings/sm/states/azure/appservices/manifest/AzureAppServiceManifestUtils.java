package software.wings.sm.states.azure.appservices.manifest;

import static io.harness.azure.model.AzureConstants.IS_SETTING_SECRET_REGEX;

import static software.wings.beans.yaml.YamlConstants.APP_SETTINGS_FILE;
import static software.wings.beans.yaml.YamlConstants.CONN_STRINGS_FILE;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConfiguration;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.Service;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.sm.ExecutionContext;
import software.wings.utils.ApplicationManifestUtils;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
@Singleton
public class AzureAppServiceManifestUtils {
  @Inject private ApplicationManifestUtils applicationManifestUtils;
  @Inject private ApplicationManifestService applicationManifestService;

  public AzureAppServiceConfiguration getAzureAppServiceConfiguration(ExecutionContext context) {
    AzureAppServiceConfiguration azureAppServiceConfiguration = new AzureAppServiceConfiguration();
    ApplicationManifest applicationManifest = getServiceApplicationManifest(context);
    populateAzureAppServiceConfiguration(applicationManifest, azureAppServiceConfiguration);

    Optional<ApplicationManifest> appSettingsOp =
        getLatestEnvOverrideByKind(context, AppManifestKind.AZURE_APP_SETTINGS_OVERRIDE);
    appSettingsOp.ifPresent(
        appManifest -> populateAzureAppServiceConfiguration(appManifest, azureAppServiceConfiguration));

    Optional<ApplicationManifest> connStringsOp =
        getLatestEnvOverrideByKind(context, AppManifestKind.AZURE_CONN_STRINGS_OVERRIDE);
    connStringsOp.ifPresent(
        appManifest -> populateAzureAppServiceConfiguration(appManifest, azureAppServiceConfiguration));

    return azureAppServiceConfiguration;
  }

  private ApplicationManifest getServiceApplicationManifest(ExecutionContext context) {
    Service service = applicationManifestUtils.fetchServiceFromContext(context);
    return applicationManifestService.getByServiceId(
        context.getAppId(), service.getUuid(), AppManifestKind.AZURE_APP_SERVICE_MANIFEST);
  }

  @NotNull
  private Optional<ApplicationManifest> getLatestEnvOverrideByKind(ExecutionContext context, AppManifestKind kind) {
    // Map is EnumMap and items should be ordered like enum defined in K8sValuesLocation class
    Map<K8sValuesLocation, ApplicationManifest> manifests =
        applicationManifestUtils.getApplicationManifests(context, kind);
    return manifests.values().stream().findFirst();
  }

  private void populateAzureAppServiceConfiguration(
      ApplicationManifest appManifest, AzureAppServiceConfiguration configuration) {
    List<ManifestFile> manifestFiles =
        applicationManifestService.getManifestFilesByAppManifestId(appManifest.getAppId(), appManifest.getUuid());
    manifestFiles.forEach(mf -> updateAppServiceConfiguration(configuration, mf));
  }

  private void updateAppServiceConfiguration(
      AzureAppServiceConfiguration serviceConfiguration, ManifestFile manifestFile) {
    if (manifestFile.getFileName().equals(CONN_STRINGS_FILE)) {
      serviceConfiguration.setConnStringsJSON(manifestFile.getFileContent());
    }

    if (manifestFile.getFileName().equals(APP_SETTINGS_FILE)) {
      serviceConfiguration.setAppSettingsJSON(manifestFile.getFileContent());
    }
  }

  public void renderAppSettings(ExecutionContext context, List<AzureAppServiceApplicationSetting> appSettings,
      ImmutableList<String> appSettingSecretsImmutableList) {
    appSettings.forEach(setting -> {
      String settingValue = setting.getValue();
      String settingName = setting.getName();
      if (appSettingSecretsImmutableList.contains(settingName)) {
        setting.setValue(extractAppServiceSettingSecretName(settingValue));
      } else {
        setting.setValue(context.renderExpression(settingValue));
      }
    });
  }

  public void renderConnStrings(ExecutionContext context, List<AzureAppServiceConnectionString> connSettings,
      ImmutableList<String> connStringSecretsImmutableList) {
    connSettings.forEach(setting -> {
      String settingValue = setting.getValue();
      String settingName = setting.getName();
      if (connStringSecretsImmutableList.contains(settingName)) {
        setting.setValue(extractAppServiceSettingSecretName(settingValue));
      } else {
        setting.setValue(context.renderExpression(settingValue));
      }
    });
  }

  private String extractAppServiceSettingSecretName(String secretExpression) {
    Matcher matcher = IS_SETTING_SECRET_REGEX.matcher(secretExpression);

    if (matcher.find()) {
      try {
        return matcher.group("secretName");
      } catch (Exception e) {
        throw new InvalidRequestException(
            format("Unable to extract secret name from secret expression: %s", secretExpression), e);
      }
    } else {
      throw new InvalidArgumentsException(format("Unable to find secret name that matches"
              + " the pattern for secret expression: %s",
          secretExpression));
    }
  }

  public ImmutableList<String> getAppSettingSecretsImmutableList(List<AzureAppServiceApplicationSetting> appSettings) {
    return appSettings.stream()
        .filter(setting -> IS_SETTING_SECRET_REGEX.asPredicate().test(setting.getValue()))
        .map(AzureAppServiceApplicationSetting::getName)
        .collect(Collectors.collectingAndThen(toList(), ImmutableList::copyOf));
  }

  public ImmutableList<String> getConnStringSecretsImmutableList(List<AzureAppServiceConnectionString> connStrings) {
    return connStrings.stream()
        .filter(setting -> IS_SETTING_SECRET_REGEX.asPredicate().test(setting.getValue()))
        .map(AzureAppServiceConnectionString::getName)
        .collect(Collectors.collectingAndThen(toList(), ImmutableList::copyOf));
  }

  public Map<String, String> renderServiceVariables(ExecutionContext context) {
    return context.getServiceVariables().entrySet().stream().collect(
        Collectors.toMap(Map.Entry::getKey, e -> context.renderExpression(e.getValue().toString())));
  }
}
