/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.azure.model;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.serializer.JsonUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Collections;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AzureAppServiceConfiguration {
  private String appSettingsJSON;
  private String connStringsJSON;

  public List<AzureAppServiceApplicationSetting> getAppSettings() {
    if (isBlank(appSettingsJSON)) {
      return Collections.emptyList();
    }

    return JsonUtils.asObject(appSettingsJSON, new TypeReference<List<AzureAppServiceApplicationSetting>>() {});
  }

  public List<AzureAppServiceConnectionString> getConnStrings() {
    if (isBlank(connStringsJSON)) {
      return Collections.emptyList();
    }

    return JsonUtils.asObject(connStringsJSON, new TypeReference<List<AzureAppServiceConnectionString>>() {});
  }
}
