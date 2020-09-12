package io.harness.delegate.beans.connector.appdynamicsconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppDynamicsConnectorDTO extends ConnectorConfigDTO implements DecryptableEntity {
  @NotNull String username;
  @NotNull String accountname;
  @NotNull String controllerUrl;
  @NotNull String accountId;

  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData passwordRef;

  public String getControllerUrl() {
    if (controllerUrl.endsWith("/")) {
      return controllerUrl;
    }
    return controllerUrl + "/";
  }
}
