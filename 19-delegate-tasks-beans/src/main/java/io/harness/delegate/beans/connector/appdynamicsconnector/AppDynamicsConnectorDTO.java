package io.harness.delegate.beans.connector.appdynamicsconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.encryption.Encrypted;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.wings.annotation.EncryptableSetting;
import software.wings.settings.SettingVariableTypes;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonTypeName("AppDynamics")
public class AppDynamicsConnectorDTO extends ConnectorConfigDTO implements EncryptableSetting {
  @NotNull String username;
  @NotNull String accountname;
  @Encrypted(fieldName = "password", isReference = true) char[] password;
  String passwordReference;
  @NotNull String controllerUrl;
  String accountId;

  @Override
  public SettingVariableTypes getSettingType() {
    return SettingVariableTypes.APP_DYNAMICS;
  }
}
