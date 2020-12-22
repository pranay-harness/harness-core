package io.harness.delegate.beans.azure.appservicesettings.value;

import static io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceSettingConstants.HARNESS_SETTING_JSON_TYPE;
import static io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceSettingValueType.HARNESS_SETTING;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(HARNESS_SETTING_JSON_TYPE)

@Data
@NoArgsConstructor
@ApiModel("AzureAppServiceHarnessSettingValue")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AzureAppServiceHarnessSettingValue implements AzureAppServiceSettingValue {
  @NotNull String value;

  @Setter(AccessLevel.NONE) AzureAppServiceSettingValueType type = HARNESS_SETTING;

  @Builder
  public AzureAppServiceHarnessSettingValue(String value) {
    this.value = value;
  }
}
