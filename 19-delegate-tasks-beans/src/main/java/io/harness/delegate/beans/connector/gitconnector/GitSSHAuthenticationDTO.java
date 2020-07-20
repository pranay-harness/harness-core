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
public class GitSSHAuthenticationDTO extends GitAuthenticationDTO {
  @JsonProperty("type") GitConnectionType gitConnectionType;
  String url;
  @Encrypted(fieldName = "sshKey", isReference = true) char[] sshKey;
  @JsonProperty("sshKeyReference") String encryptedSshKey;
  String branchName;
  String accountId;

  @Override
  public SettingVariableTypes getSettingType() {
    return SettingVariableTypes.GIT_NG;
  }
}
