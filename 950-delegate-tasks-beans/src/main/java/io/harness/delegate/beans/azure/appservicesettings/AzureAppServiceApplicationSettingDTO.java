/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.azure.appservicesettings;

import static io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceSettingConstants.APPLICATION_SETTING_JSON_TYPE;

import io.harness.security.encryption.EncryptedRecord;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(APPLICATION_SETTING_JSON_TYPE)

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("AzureAppServiceApplicationSetting")
public class AzureAppServiceApplicationSettingDTO extends AzureAppServiceSettingDTO {
  @Builder
  public AzureAppServiceApplicationSettingDTO(
      String name, String value, boolean sticky, EncryptedRecord encryptedRecord, String accountId) {
    super(name, value, sticky, encryptedRecord, accountId);
  }
}
