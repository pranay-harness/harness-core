package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.encryption.Encrypted;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import software.wings.settings.SettingVariableTypes;

@Value
@Builder
@JsonTypeName("OpenIdConnect")
public class KubernetesOpenIdConnectDTO extends KubernetesAuthCredentialDTO {
  String oidcIssuerUrl;

  @Getter(onMethod = @__(@JsonIgnore))
  @JsonIgnore
  @ApiModelProperty(hidden = true)
  @Encrypted(fieldName = "oidcClientId", isReference = true)
  char[] oidcClientId;
  @JsonProperty("oidcClientIdRef") String encryptedOidcClientId;

  String oidcUsername;

  @Getter(onMethod = @__(@JsonIgnore))
  @JsonIgnore
  @ApiModelProperty(hidden = true)
  @Encrypted(fieldName = "oidcPassword", isReference = true)
  char[] oidcPassword;
  @JsonProperty("oidcPasswordRef") String encryptedOidcPassword;

  @Getter(onMethod = @__(@JsonIgnore))
  @JsonIgnore
  @ApiModelProperty(hidden = true)
  @Encrypted(fieldName = "oidcSecret", isReference = true)
  char[] oidcSecret;
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