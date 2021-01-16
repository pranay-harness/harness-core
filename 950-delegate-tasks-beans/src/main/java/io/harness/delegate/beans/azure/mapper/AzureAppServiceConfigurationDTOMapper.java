package io.harness.delegate.beans.azure.mapper;

import static io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceSettingValueType.AZURE_SETTING;
import static io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceSettingValueType.HARNESS_SETTING;
import static io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceSettingValueType.HARNESS_SETTING_SECRET;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.azure.model.AzureAppServiceDockerSetting;
import io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceApplicationSettingDTO;
import io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceConnectionStringDTO;
import io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceDockerSettingDTO;
import io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceAzureSettingValue;
import io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceHarnessSettingSecretRef;
import io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceHarnessSettingSecretValue;
import io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceHarnessSettingValue;
import io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceSettingValue;
import io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceSettingValueType;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AzureAppServiceConfigurationDTOMapper {
  public Map<String, AzureAppServiceApplicationSetting> getAzureAppServiceAppSettings(
      Map<String, AzureAppServiceApplicationSettingDTO> appSettingDTOs) {
    return appSettingDTOs.entrySet().stream().collect(
        Collectors.toMap(Map.Entry::getKey, entry -> toApplicationSetting(entry.getKey(), entry.getValue())));
  }

  private AzureAppServiceApplicationSetting toApplicationSetting(
      final String settingName, AzureAppServiceApplicationSettingDTO applicationSettingDTO) {
    Objects.requireNonNull(
        applicationSettingDTO, format("Application setting can't be null, settingName: %s", settingName));
    AzureAppServiceSettingValue azureAppServiceSettingValue = applicationSettingDTO.getValue();
    String value = getPlainSettingValue(azureAppServiceSettingValue);

    return AzureAppServiceApplicationSetting.builder()
        .name(applicationSettingDTO.getName())
        .sticky(applicationSettingDTO.isSticky())
        .value(value)
        .build();
  }

  public Map<String, AzureAppServiceConnectionString> getAzureAppServiceConnStrings(
      Map<String, AzureAppServiceConnectionStringDTO> connStringDTOs) {
    return connStringDTOs.entrySet().stream().collect(
        Collectors.toMap(Map.Entry::getKey, entry -> toConnectionString(entry.getKey(), entry.getValue())));
  }

  private AzureAppServiceConnectionString toConnectionString(
      final String settingName, AzureAppServiceConnectionStringDTO connectionStringDTO) {
    Objects.requireNonNull(
        connectionStringDTO, format("Connection string can't be null, settingName: %s", settingName));
    AzureAppServiceSettingValue azureAppServiceSettingValue = connectionStringDTO.getValue();
    String value = getPlainSettingValue(azureAppServiceSettingValue);

    return AzureAppServiceConnectionString.builder()
        .name(connectionStringDTO.getName())
        .sticky(connectionStringDTO.isSticky())
        .type(connectionStringDTO.getType())
        .value(value)
        .build();
  }

  public Map<String, AzureAppServiceDockerSetting> getAzureAppServiceDockerSettings(
      Map<String, AzureAppServiceDockerSettingDTO> dockerSettingDTOs) {
    return dockerSettingDTOs.entrySet().stream().collect(
        Collectors.toMap(Map.Entry::getKey, entry -> toDockerSetting(entry.getKey(), entry.getValue())));
  }

  private AzureAppServiceDockerSetting toDockerSetting(
      final String settingName, AzureAppServiceDockerSettingDTO dockerSettingDTO) {
    Objects.requireNonNull(dockerSettingDTO, format("Docker setting can't be null, settingName: %s", settingName));
    AzureAppServiceSettingValue azureAppServiceSettingValue = dockerSettingDTO.getValue();
    String value = getPlainSettingValue(azureAppServiceSettingValue);

    return AzureAppServiceDockerSetting.builder()
        .name(dockerSettingDTO.getName())
        .sticky(dockerSettingDTO.isSticky())
        .value(value)
        .build();
  }

  private String getPlainSettingValue(AzureAppServiceSettingValue azureAppServiceSettingValue) {
    if (azureAppServiceSettingValue instanceof AzureAppServiceHarnessSettingSecretValue) {
      char[] decryptedValue = ((AzureAppServiceHarnessSettingSecretValue) azureAppServiceSettingValue)
                                  .getSettingSecretRef()
                                  .getPasswordRef()
                                  .getDecryptedValue();
      return new String(decryptedValue);
    } else if (azureAppServiceSettingValue instanceof AzureAppServiceHarnessSettingValue) {
      return ((AzureAppServiceHarnessSettingValue) azureAppServiceSettingValue).getValue();
    } else if (azureAppServiceSettingValue instanceof AzureAppServiceAzureSettingValue) {
      return ((AzureAppServiceAzureSettingValue) azureAppServiceSettingValue).getDecryptedValue();
    } else {
      throw new InvalidRequestException(
          format("Unsupported Azure App Service setting value type, type: %s", azureAppServiceSettingValue.getClass()));
    }
  }

  public Map<String, AzureAppServiceApplicationSettingDTO> getAzureAppServiceAppSettingDTOs(
      Map<String, AzureAppServiceApplicationSetting> appSettings, AzureAppServiceSettingValueType type) {
    return appSettings.entrySet().stream().collect(
        Collectors.toMap(Map.Entry::getKey, entry -> toApplicationSettingDTO(entry.getValue(), type)));
  }

  public AzureAppServiceApplicationSettingDTO toApplicationSettingDTO(
      AzureAppServiceApplicationSetting applicationSetting, AzureAppServiceSettingValueType type) {
    String name = applicationSetting.getName();
    String value = applicationSetting.getValue();
    if (isBlank(name)) {
      throw new IllegalArgumentException("Application setting name can't be null or empty");
    }

    AzureAppServiceSettingValue appSettingValue = buildAzureAppServiceSettingValue(value, type);

    return AzureAppServiceApplicationSettingDTO.builder()
        .name(name)
        .sticky(applicationSetting.isSticky())
        .value(appSettingValue)
        .build();
  }

  public Map<String, AzureAppServiceConnectionStringDTO> getAzureAppServiceConnStringDTOs(
      Map<String, AzureAppServiceConnectionString> connSettings, AzureAppServiceSettingValueType type) {
    return connSettings.entrySet().stream().collect(
        Collectors.toMap(Map.Entry::getKey, entry -> toConnectionSettingDTO(entry.getValue(), type)));
  }

  public AzureAppServiceConnectionStringDTO toConnectionSettingDTO(
      AzureAppServiceConnectionString connectionString, AzureAppServiceSettingValueType type) {
    String name = connectionString.getName();
    String value = connectionString.getValue();
    if (isBlank(name)) {
      throw new IllegalArgumentException("Connection string name can't be null or empty");
    }

    AzureAppServiceSettingValue connectionStringValue = buildAzureAppServiceSettingValue(value, type);

    return AzureAppServiceConnectionStringDTO.builder()
        .name(name)
        .sticky(connectionString.isSticky())
        .type(connectionString.getType())
        .value(connectionStringValue)
        .build();
  }

  public Map<String, AzureAppServiceDockerSettingDTO> getAzureAppServiceDockerSettingDTOs(
      Map<String, AzureAppServiceDockerSetting> dockerSettings, AzureAppServiceSettingValueType type) {
    return dockerSettings.entrySet().stream().collect(
        Collectors.toMap(Map.Entry::getKey, entry -> toDockerSettingDTO(entry.getValue(), type)));
  }

  public AzureAppServiceDockerSettingDTO toDockerSettingDTO(
      AzureAppServiceDockerSetting dockerSetting, AzureAppServiceSettingValueType type) {
    String name = dockerSetting.getName();
    String value = dockerSetting.getValue();
    if (isBlank(name)) {
      throw new IllegalArgumentException("Docker settings name or value can't be null or empty");
    }

    AzureAppServiceSettingValue dockerSettingValue = buildAzureAppServiceSettingValue(value, type);

    return AzureAppServiceDockerSettingDTO.builder()
        .name(name)
        .sticky(dockerSetting.isSticky())
        .value(dockerSettingValue)
        .build();
  }

  public AzureAppServiceSettingValue buildAzureAppServiceSettingValue(
      String settingValueOrRef, AzureAppServiceSettingValueType type) {
    if (HARNESS_SETTING == type) {
      return AzureAppServiceHarnessSettingValue.builder().value(settingValueOrRef).build();
    } else if (HARNESS_SETTING_SECRET == type) {
      SecretRefData secretRefData = new SecretRefData(settingValueOrRef, Scope.ACCOUNT, null);
      AzureAppServiceHarnessSettingSecretRef settingSecretRef =
          AzureAppServiceHarnessSettingSecretRef.builder().passwordRef(secretRefData).build();
      return AzureAppServiceHarnessSettingSecretValue.builder().settingSecretRef(settingSecretRef).build();
    } else if (AZURE_SETTING == type) {
      return AzureAppServiceAzureSettingValue.builder().decryptedValue(settingValueOrRef).build();
    } else {
      throw new IllegalArgumentException(format("Unsupported Azure App Service setting value type, type: %s", type));
    }
  }
}
