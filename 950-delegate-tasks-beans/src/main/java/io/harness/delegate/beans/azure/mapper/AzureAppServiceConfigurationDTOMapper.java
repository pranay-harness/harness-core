/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.azure.mapper;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceApplicationSettingDTO;
import io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceConnectionStringDTO;

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

    return AzureAppServiceApplicationSetting.builder()
        .name(applicationSettingDTO.getName())
        .sticky(applicationSettingDTO.isSticky())
        .value(applicationSettingDTO.getValue())
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

    return AzureAppServiceConnectionString.builder()
        .name(connectionStringDTO.getName())
        .sticky(connectionStringDTO.isSticky())
        .type(connectionStringDTO.getType())
        .value(connectionStringDTO.getValue())
        .build();
  }

  public Map<String, AzureAppServiceApplicationSettingDTO> getAzureAppServiceAppSettingDTOs(
      Map<String, AzureAppServiceApplicationSetting> appSettings) {
    return appSettings.entrySet().stream().collect(
        Collectors.toMap(Map.Entry::getKey, entry -> toApplicationSettingDTO(entry.getValue())));
  }

  public AzureAppServiceApplicationSettingDTO toApplicationSettingDTO(
      AzureAppServiceApplicationSetting applicationSetting) {
    String name = applicationSetting.getName();
    if (isBlank(name)) {
      throw new IllegalArgumentException("Application setting name can't be null or empty");
    }

    return AzureAppServiceApplicationSettingDTO.builder()
        .name(name)
        .sticky(applicationSetting.isSticky())
        .value(applicationSetting.getValue())
        .build();
  }

  public Map<String, AzureAppServiceConnectionStringDTO> getAzureAppServiceConnStringDTOs(
      Map<String, AzureAppServiceConnectionString> connSettings) {
    return connSettings.entrySet().stream().collect(
        Collectors.toMap(Map.Entry::getKey, entry -> toConnectionSettingDTO(entry.getValue())));
  }

  public AzureAppServiceConnectionStringDTO toConnectionSettingDTO(AzureAppServiceConnectionString connectionString) {
    String name = connectionString.getName();
    if (isBlank(name)) {
      throw new IllegalArgumentException("Connection string name can't be null or empty");
    }

    return AzureAppServiceConnectionStringDTO.builder()
        .name(name)
        .sticky(connectionString.isSticky())
        .type(connectionString.getType())
        .value(connectionString.getValue())
        .build();
  }
}
