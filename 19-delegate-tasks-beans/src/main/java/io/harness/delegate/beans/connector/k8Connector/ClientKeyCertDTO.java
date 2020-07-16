package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import software.wings.settings.SettingVariableTypes;

@Data
@Builder
public class ClientKeyCertDTO extends KubernetesAuthCredentialDTO {
  @Encrypted(fieldName = "clientCert", isReference = true) char[] clientCert;
  @JsonProperty("clientCertRef") String encryptedClientCert;

  @Encrypted(fieldName = "clientKey", isReference = true) char[] clientKey;
  @JsonProperty("clientKeyRef") String encryptedClientKey;

  @Encrypted(fieldName = "clientKeyPassphrase", isReference = true) char[] clientKeyPassphrase;
  @JsonProperty("clientKeyPassphraseRef") String encryptedClientKeyPassphrase;

  @JsonProperty("clientKeyAlgo") String clientKeyAlgo;

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