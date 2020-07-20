package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesAuthDTO {
  @JsonProperty("type") KubernetesAuthType authType;

  @Builder
  public KubernetesAuthDTO(KubernetesAuthType authType, KubernetesAuthCredentialDTO credentials) {
    this.authType = authType;
    this.credentials = credentials;
  }

  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @JsonSubTypes({
    @JsonSubTypes.Type(value = KubernetesUserNamePasswordDTO.class, name = "UsernamePassword")
    , @JsonSubTypes.Type(value = KubernetesClientKeyCertDTO.class, name = "ClientKeyCert"),
        @JsonSubTypes.Type(value = KubernetesServiceAccountDTO.class, name = "ServiceAccount"),
        @JsonSubTypes.Type(value = KubernetesOpenIdConnectDTO.class, name = "OpenIdConnect")
  })
  KubernetesAuthCredentialDTO credentials;
}
