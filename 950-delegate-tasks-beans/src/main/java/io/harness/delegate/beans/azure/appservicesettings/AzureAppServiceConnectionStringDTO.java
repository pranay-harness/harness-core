package io.harness.delegate.beans.azure.appservicesettings;

import static io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceSettingConstants.CONNECTION_SETTING_JSON_TYPE;

import io.harness.azure.model.AzureAppServiceConnectionStringType;
import io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceSettingValue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(CONNECTION_SETTING_JSON_TYPE)

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("AzureAppServiceConnectionString")
public class AzureAppServiceConnectionStringDTO extends AzureAppServiceSettingDTO {
  @NotNull AzureAppServiceConnectionStringType type;

  @Builder
  public AzureAppServiceConnectionStringDTO(
      String name, AzureAppServiceSettingValue value, boolean sticky, AzureAppServiceConnectionStringType type) {
    super(name, value, sticky);
    this.type = type;
  }
}
