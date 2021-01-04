package io.harness.delegate.beans.azure.appservicesettings.value;

import static io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceSettingConstants.HARNESS_SETTING_SECRET_REF_JSON_TYPE;

import io.harness.beans.DecryptableEntity;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(HARNESS_SETTING_SECRET_REF_JSON_TYPE)

@Data
@NoArgsConstructor
@ApiModel("AzureAppServiceHarnessSettingSecretRef")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AzureAppServiceHarnessSettingSecretRef implements DecryptableEntity {
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData secretRef;

  @Builder
  public AzureAppServiceHarnessSettingSecretRef(SecretRefData secretRef) {
    this.secretRef = secretRef;
  }
}
