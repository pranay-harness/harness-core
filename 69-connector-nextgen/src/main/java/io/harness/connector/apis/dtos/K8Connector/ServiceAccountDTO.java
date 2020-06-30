package io.harness.connector.apis.dtos.K8Connector;

import lombok.Builder;
import lombok.Data;
import org.codehaus.jackson.annotate.JsonProperty;

@Data
@Builder
public class ServiceAccountDTO extends KubernetesAuthCredentialDTO {
  @JsonProperty("service-acccount-token") String serviceAccountToken;
}
