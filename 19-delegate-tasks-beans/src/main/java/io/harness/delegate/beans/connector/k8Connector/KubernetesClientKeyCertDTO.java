package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.hibernate.validator.constraints.NotBlank;
import software.wings.settings.SettingVariableTypes;

@Data
@Builder
@JsonTypeName("ClientKeyCert")
public class KubernetesClientKeyCertDTO extends KubernetesAuthCredentialDTO {
  @Getter(onMethod = @__(@JsonIgnore))
  @JsonIgnore
  @Encrypted(fieldName = "clientCert", isReference = true)
  char[] clientCert;
  @JsonProperty("clientCertRef") @NotBlank String encryptedClientCert;

  @Getter(onMethod = @__(@JsonIgnore))
  @JsonIgnore
  @Encrypted(fieldName = "clientKey", isReference = true)
  char[] clientKey;
  @JsonProperty("clientKeyRef") @NotBlank String encryptedClientKey;

  @Getter(onMethod = @__(@JsonIgnore))
  @JsonIgnore
  @Encrypted(fieldName = "clientKeyPassphrase", isReference = true)
  char[] clientKeyPassphrase;
  @JsonProperty("clientKeyPassphraseRef") @NotBlank String encryptedClientKeyPassphrase;

  String clientKeyAlgo;

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