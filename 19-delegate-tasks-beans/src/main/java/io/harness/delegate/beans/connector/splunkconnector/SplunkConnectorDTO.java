package io.harness.delegate.beans.connector.splunkconnector;

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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonTypeName("Splunk")
public class SplunkConnectorDTO extends ConnectorConfigDTO implements EncryptableSetting {
  String splunkUrl;
  String username;
  @Encrypted(fieldName = "passwordReference", isReference = true) char[] password;
  String passwordReference;
  String accountId;

  @Override
  public SettingVariableTypes getSettingType() {
    return SettingVariableTypes.SPLUNK;
  }
}
