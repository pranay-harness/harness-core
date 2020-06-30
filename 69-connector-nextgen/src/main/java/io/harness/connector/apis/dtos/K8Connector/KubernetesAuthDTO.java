package io.harness.connector.apis.dtos.K8Connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.connector.common.kubernetes.KubernetesAuthType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesAuthDTO {
  @JsonProperty("type") KubernetesAuthType authType;
  @JsonProperty("type1") KubernetesAuthType authType1;

  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type1", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @JsonSubTypes({
    @JsonSubTypes.Type(value = UserNamePasswordDTO.class, name = "UsernamePassword")
    , @JsonSubTypes.Type(value = ClientKeyCertDTO.class, name = "ClientKeyCert"),
        @JsonSubTypes.Type(value = ServiceAccountDTO.class, name = "ServiceAccount"),
        @JsonSubTypes.Type(value = ClientKeyCertDTO.class, name = "OpenIdConnect")
  })
  KubernetesAuthCredentialDTO credentials;
}
