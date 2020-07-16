package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import software.wings.settings.SettingVariableTypes;

@Data
@Builder
public class ServiceAccountDTO extends KubernetesAuthCredentialDTO {
  @Encrypted(fieldName = "serviceAccountToken", isReference = true) char[] serviceAccountToken;
  @JsonProperty("serviceAcccountTokenRef") String encryptedServiceAccountToken;

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