package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Value;
import software.wings.settings.SettingVariableTypes;

@Value
@Builder
public class OpenIdConnectDTO extends KubernetesAuthCredentialDTO {
  String oidcIssuerUrl;

  @Encrypted(fieldName = "oidcClientId", isReference = true) char[] oidcClientId;
  @JsonProperty("oidcClientIdRef") String encryptedOidcClientId;

  String oidcUsername;

  @Encrypted(fieldName = "oidcPassword", isReference = true) char[] oidcPassword;
  @JsonProperty("oidcPasswordRef") String encryptedOidcPassword;

  @Encrypted(fieldName = "oidcSecret", isReference = true) char[] oidcSecret;
  @JsonProperty("oidcSecretRef") String encryptedOidcSecret;

  String oidcScopes;

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