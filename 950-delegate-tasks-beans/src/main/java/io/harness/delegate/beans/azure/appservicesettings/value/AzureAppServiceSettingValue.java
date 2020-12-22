package io.harness.delegate.beans.azure.appservicesettings.value;

import static io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceSettingConstants.AZURE_SETTING_JSON_TYPE;
import static io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceSettingConstants.HARNESS_SETTING_JSON_TYPE;
import static io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceSettingConstants.HARNESS_SETTING_SECRET_JSON_TYPE;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = AzureAppServiceAzureSettingValue.class, name = AZURE_SETTING_JSON_TYPE)
  , @JsonSubTypes.Type(value = AzureAppServiceHarnessSettingSecretValue.class, name = HARNESS_SETTING_SECRET_JSON_TYPE),
      @JsonSubTypes.Type(value = AzureAppServiceHarnessSettingValue.class, name = HARNESS_SETTING_JSON_TYPE)
})

@ApiModel("AzureAppServiceSettingValue")
public interface AzureAppServiceSettingValue {}
