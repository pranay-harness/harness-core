package io.harness.delegate.beans.connector.k8Connector;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import lombok.Data;

@Data
@JsonSubTypes({
  @JsonSubTypes.Type(value = KubernetesUserNamePasswordDTO.class, name = KubernetesConfigConstants.USERNAME_PASSWORD)
  , @JsonSubTypes.Type(value = KubernetesServiceAccountDTO.class, name = KubernetesConfigConstants.SERVICE_ACCOUNT),
      @JsonSubTypes.Type(value = KubernetesOpenIdConnectDTO.class, name = KubernetesConfigConstants.OPENID_CONNECT),
      @JsonSubTypes.Type(value = KubernetesClientKeyCertDTO.class, name = KubernetesConfigConstants.CLIENT_KEY_CERT)
})
public abstract class KubernetesAuthCredentialDTO implements DecryptableEntity {}
