package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClientKeyCertDTO extends KubernetesAuthCredentialDTO {
  @JsonProperty("client-cert") String clientCert;
  @JsonProperty("client-key") String clientKey;
  @JsonProperty("client-key-passphrase") String clientKeyPassphrase;
  @JsonProperty("clientKeyAlgo") String clientKeyAlgo;
}
