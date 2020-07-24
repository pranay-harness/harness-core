package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.encryption.Encrypted;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import software.wings.settings.SettingVariableTypes;

@Data
@Builder
@JsonTypeName("ServiceAccount")
public class KubernetesServiceAccountDTO extends KubernetesAuthCredentialDTO {
  @Getter(onMethod = @__(@JsonIgnore))
  @JsonIgnore
  @Encrypted(fieldName = "serviceAccountToken", isReference = true)
  protected char[] serviceAccountToken;

  @JsonProperty("serviceAccountTokenRef") String encryptedServiceAccountToken;
  @ApiModelProperty(hidden = true) String accountId;

  private static final String DUMMY_ACCOUNT_ID = "AccountId";

  @Override
  public SettingVariableTypes getSettingType() {
    return SettingVariableTypes.KUBERNETES_CLUSTER_NG;
  }
}