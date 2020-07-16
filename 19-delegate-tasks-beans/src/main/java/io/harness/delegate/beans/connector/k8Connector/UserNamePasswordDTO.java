package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import software.wings.settings.SettingVariableTypes;

@Data
@Builder
public class UserNamePasswordDTO extends KubernetesAuthCredentialDTO {
  String username;
  @Encrypted(fieldName = "password", isReference = true) char[] password;
  @JsonProperty("passwordRef") String encryptedPassword;

  // todo @deepak: Remove this field if not required
  private String cacert;

  private static final String DUMMY_ACCOUNT_ID = "AccountId";

  @Override
  public SettingVariableTypes getSettingType() {
    return SettingVariableTypes.KUBERNETES_CLUSTER_NG;
  }

  @Override
  public String getAccountId() {
    // todo @deepak Remove this dummy accountId
    return DUMMY_ACCOUNT_ID;
  }

  @Override
  public void setAccountId(String accountId) {
    // todo @deepak Remove this dummy set method
  }
}