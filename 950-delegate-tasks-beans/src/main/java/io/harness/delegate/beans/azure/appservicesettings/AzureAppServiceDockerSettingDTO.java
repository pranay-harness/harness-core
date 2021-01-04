package io.harness.delegate.beans.azure.appservicesettings;

import static io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceSettingConstants.DOCKER_SETTING_JSON_TYPE;

import io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceSettingValue;

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
@JsonTypeName(DOCKER_SETTING_JSON_TYPE)

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("AzureAppServiceDockerSetting")
public class AzureAppServiceDockerSettingDTO extends AzureAppServiceSettingDTO {
  @Builder
  public AzureAppServiceDockerSettingDTO(String name, AzureAppServiceSettingValue value, boolean sticky) {
    super(name, value, sticky);
  }
}
