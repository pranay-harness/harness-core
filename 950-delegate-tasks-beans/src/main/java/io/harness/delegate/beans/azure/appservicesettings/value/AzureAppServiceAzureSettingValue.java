package io.harness.delegate.beans.azure.appservicesettings.value;

import static io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceSettingConstants.AZURE_SETTING_JSON_TYPE;

import io.harness.security.encryption.EncryptedRecord;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(AZURE_SETTING_JSON_TYPE)

@Data
@NoArgsConstructor
@ApiModel("AzureAppServiceAzureSettingValue")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AzureAppServiceAzureSettingValue implements AzureAppServiceSettingValue {
  EncryptedRecord encryptedRecord;
  String accountId;
  String decryptedValue;

  @Setter(AccessLevel.NONE) AzureAppServiceSettingValueType type = AzureAppServiceSettingValueType.AZURE_SETTING;

  @Builder
  public AzureAppServiceAzureSettingValue(EncryptedRecord encryptedRecord, String accountId, String decryptedValue) {
    this.encryptedRecord = encryptedRecord;
    this.accountId = accountId;
    this.decryptedValue = decryptedValue;
  }
}
