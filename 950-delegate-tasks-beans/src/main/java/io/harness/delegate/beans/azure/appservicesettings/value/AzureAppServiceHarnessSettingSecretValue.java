package io.harness.delegate.beans.azure.appservicesettings.value;

import static io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceSettingConstants.HARNESS_SETTING_SECRET_JSON_TYPE;
import static io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceSettingValueType.HARNESS_SETTING_SECRET;

import io.harness.security.encryption.EncryptedDataDetail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(HARNESS_SETTING_SECRET_JSON_TYPE)

@Data
@NoArgsConstructor
@ApiModel("AzureAppServiceHarnessSettingSecretValue")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AzureAppServiceHarnessSettingSecretValue implements AzureAppServiceSettingValue {
  @NotNull List<EncryptedDataDetail> encryptedDataDetails;
  @NotNull AzureAppServiceHarnessSettingSecretRef settingSecretRef;

  @Setter(AccessLevel.NONE) AzureAppServiceSettingValueType type = HARNESS_SETTING_SECRET;

  @Builder
  public AzureAppServiceHarnessSettingSecretValue(
      AzureAppServiceHarnessSettingSecretRef settingSecretRef, List<EncryptedDataDetail> encryptedDataDetails) {
    this.settingSecretRef = settingSecretRef;
    this.encryptedDataDetails = encryptedDataDetails;
  }
}
