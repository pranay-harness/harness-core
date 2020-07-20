package io.harness.delegate.beans.connector.gitconnector;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.settings.SettingVariableTypes;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class GitHTTPAuthenticationDTO extends GitAuthenticationDTO {
  @JsonProperty("type") GitConnectionType gitConnectionType;
  String url;
  String username;
  @Encrypted(fieldName = "password", isReference = true) char[] password;
  @JsonProperty("passwordReference") String encryptedPassword;
  String branchName;
  String accountId;

  @Override
  public SettingVariableTypes getSettingType() {
    return SettingVariableTypes.GIT_NG;
  }
}
