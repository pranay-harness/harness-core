/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.azure.appservicesettings;

import static io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceSettingConstants.APPLICATION_SETTING_JSON_TYPE;
import static io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceSettingConstants.CONNECTION_SETTING_JSON_TYPE;

import io.harness.security.encryption.EncryptedRecord;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = AzureAppServiceApplicationSettingDTO.class, name = APPLICATION_SETTING_JSON_TYPE)
  , @JsonSubTypes.Type(value = AzureAppServiceConnectionStringDTO.class, name = CONNECTION_SETTING_JSON_TYPE)
})

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("AzureAppServiceSettingDTO")
public abstract class AzureAppServiceSettingDTO {
  @NotNull protected String name;
  @NotNull protected String value;
  protected boolean sticky;
  EncryptedRecord encryptedRecord;
  String accountId;
}
