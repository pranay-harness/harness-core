package io.harness.connector.apis.dtos.K8Connector;

import lombok.Builder;
import lombok.Data;
import org.codehaus.jackson.annotate.JsonProperty;

@Data
@Builder
public class ClientKeyCertDTO extends KubernetesAuthCredentialDTO {
  @JsonProperty("client-cert") String clientCert;
  @JsonProperty("client-key") String clientKey;
  @JsonProperty("client-key-passphrase") String clientKeyPassphrase;
  @JsonProperty("clientKeyAlgo") String clientKeyAlgo;
}
